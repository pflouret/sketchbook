require 'jruby_art'
require 'jruby_art/app'
require 'toxiclibs'

require_relative '../_lib/pflib/pflib'
require 'jruby/core_ext'

class PolyLines < PF::ProcessingApp

    load_library :controlP5
    include_package 'controlP5'

    def setup
        super

        build_gui
        reset

        @squish = true
        @squiggle_factor = 0.3
        @spacing = 4
        @scale = 1.45
        @c = nil
    end

    def draw
        clear

        if @cur_shape.nil?
            cursor(ARROW)
        else
            cursor(HAND)
            @cur_shape.pos mouse_x, mouse_y
        end

        @shapes.each do |shape|
            shape.redraw_if_needed
            image shape.g, shape.x, shape.y
        end

        push_style
        fill 0, 5
        stroke 0, 30
        @gfx.polygon2_d @poly
        stroke_weight 8
        @poly.vertices.each { |v| point v.x, v.y }
        pop_style
    end

    def build_shape
        @cur_shape = ShapeLines.new(@poly, @r.rand(0.3..0.6), @r.rand(2..4), 1.45)
        @shapes.push @cur_shape
        @poly = Toxi::Polygon2D.new
    end

    def reset
        @poly = Toxi::Polygon2D.new
        @shapes = []
        @cur_shape = nil
        @last_shape = nil
    end

    def mouse_pressed
        if @cur_shape.nil?
            @poly.add(mouse_x, mouse_y)
        else
            @last_shape = @cur_shape
            @cur_shape = nil
        end
    end

    def key_pressed(e=nil)
        shape = @cur_shape || @last_shape
        return if ('1'..'6').cover?(key) && shape.nil?

        case key
            when ' '
                if @cur_shape.nil?
                    build_shape
                else
                    @last_shape = @cur_shape
                    @cur_shape = nil
                end
            when "\b"
                @poly.vertices.remove(@poly.vertices.size-1)
            when 'c'
                reset
                clear
            when '1', '2'
                shape.spacing = max(0.5, shape.spacing + sign('1')*0.5)
            when '3', '4'
                shape.squiggle_factor = max(0.1, shape.squiggle_factor + sign('3')*0.1)
            when '5', '6'
                shape.scale = shape.scale + sign('5')*0.05
            when 't'
                #squish = !squish
            when 'R'
                shape.reset_seed true unless shape.nil?
            else
                super
        end
        sketch_title "#{@spacing.round(1)} #{@squiggle_factor.round(1)} #{@scale.round(2)} #{@paused}"
    end

    def sign(c)
        key == c ? -1 : 1
    end

    def build_gui
        super do |cp, w|
            cp.add_color_wheel('c', w.width-200, 0, 200)
              .on_change do |e|
                shape = @cur_shape || @last_shape
                shape.line_color = color(e.get_controller.get_value.to_i) unless shape.nil?
            end

            #cp.add_slider('a')
            #    .set_value(0.0)
            #    .set_range(0.0, 2*PI)
            #    .set_number_of_tick_marks(40)
            #    .snap_to_tick_marks(true)
            #    .set_height(19)
            #    .update_size
            #    .on_change { |e| @phi = e.get_controller.get_value }
        end
    end

    def line_color(c)
        shape = @cur_shape || @last_shape
        shape.line_color = color(c.to_i) unless shape.nil?
    end
end

class ShapeLines
    attr_accessor :squiggle_factor, :spacing, :scale, :x, :y, :line_color
    attr_reader :g, :w, :h

    def initialize(poly, squiggle_factor, spacing, scale)
        @w, @h = $app.width, $app.height
        self.x, self.y = 0, 0
        @squiggle_factor, @spacing, @scale = squiggle_factor, spacing, scale
        @poly = poly
        @line_color = $app.color(0, 130)

        @seed = Random.rand(99999999)

        @needs_redraw = false
        draw_shape
    end

    def x=(new_x)
        @x = new_x - @w/2
    end

    def y=(new_y)
        @y = new_y - @h/2
    end

    def pos(x, y)
        self.x, self.y = x, y
    end

    def squiggle_factor=(s)
        @squiggle_factor = s
        @needs_redraw = true
    end

    def spacing=(s)
        @spacing = s
        @needs_redraw = true
    end

    def scale=(s)
        @scale = s
        @needs_redraw = true
    end

    def line_color=(c)
        @line_color = c
        @needs_redraw = true
    end

    def rand(*args)
        @r.rand(*args)
    end

    def reset_seed(new_seed=false)
        @seed = Random.new 99999999 if new_seed
        @r = Random.new @seed
    end

    def redraw_if_needed
        if @needs_redraw
            @needs_redraw = false
            draw_shape
        end
    end

    private

    def lines_layer
        lines = $app.create_graphics @w, @h
        lines.begin_draw
        lines.no_fill
        lines.stroke_weight 0.5
        lines.stroke @line_color

        lines.humanline_fill(squiggle_factor: @squiggle_factor, spacing: @spacing, scale: @scale, squish: true, r: @r)

        lines
    end

    def draw_shape
        reset_seed

        lines = lines_layer
        lines.load_pixels
        g = $app.create_graphics @w, @h
        g.begin_draw
        g.no_fill
        g.load_pixels
        @poly.center(TVec2D.new(@w/2, @h/2))
        (0...g.pixels.length).each do |i|
            g.pixels[i] = lines.pixels[i] if @poly.contains_point(TVec2D.new(i%@w, i/@h))
        end
        g.update_pixels
        g.end_draw
        g

        @g = g
    end

end

PolyLines.new unless defined? $app
