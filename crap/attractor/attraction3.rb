require 'jruby_art'
require 'jruby_art/app'
require 'toxiclibs'

require_relative '../pflib'

class Attraction < Processing::App

    load_library :controlP5
    include_package 'controlP5'

    def settings
        size 800, 800
    end

    def setup
        sketch_title ''
        no_fill

        new_seed
        #build_gui

        stroke_weight 1
        stroke 0, 100

        @gfx = Gfx::ToxiclibsSupport.new(self)
        @looping = true

        @n = 4
        @scale = 0.3

        init

        #no_loop
    end

    def init
        @grids = @n.times.collect do |i|
            AttractionGrid.new(width, height, create_graphics(width, height), @scale)
        end
    end

    def draw
        clear
        pg_width = @scale*width
        padding = (width - sqrt(@n)*pg_width) / 4
        x = padding
        y = padding
        @grids.each_with_index do |g, i|
            pg = g.draw
            image(pg, x, y)

            x += pg_width + padding

            if (i+1) % sqrt(@n) == 0
                x = padding
                y += pg_width + padding
            end
        end
    end

    def clear
        background 255
    end

    def mouse_pressed
        #@attractors << Attractor.new(mouse_x, mouse_y, @r.rand(200..300))
    end

    def key_pressed
        case key
            when 'p'
                if @looping then no_loop else loop end
                @looping = !@looping
            when 'z', 'x'
            when 'a', 's'
            when 'c'
                clear
                init
            when 'r'
                new_seed
            when 'S'
                screenshot @seed
            else
        end

        sketch_title "#{@looping ? '' : 'paused'}"
        redraw
    end

    def new_seed
        @seed = Random.rand 99999
        @r = Random.new(@seed)
        noise_seed @seed
    end

    def build_gui
        puts ControlKey
        Controller::autoSpacing = [1, 1]

        @cp = ControlP5.new self

        @cp.begin 0, 0
        @cp.add_button('pause')
            .set_value(0)
            .set_position(0, 0)
            .set_switch(true)
            .update_size
            .on_click do |e|
                button = e.get_controller
                on = button.is_on
                button.set_label(on ? 'resume' : 'pause')
                button.update_size
                if on then no_loop else loop end
            end

        @cp.add_button('clear')
            .set_value(0)
            .update_size
            .plug_to(self)


        #@cp.add_slider('a')
        #    .set_value(0.0)
        #    .set_range(0.0, 2*PI)
        #    .set_number_of_tick_marks(40)
        #    .snap_to_tick_marks(true)
        #    .set_height(19)
        #    .update_size
        #    .on_change { |e| @phi = e.get_controller.get_value }

        @cp.end
    end

    class Node
        attr_reader :v, :x, :y

        def initialize(x, y, velocity=TVec2D.new(0, 0), dampening=1)
            @v = TVec2D.new x, y
            @velocity = velocity
            @dampening = dampening
        end

        def update
            return unless @v.is_in_rectangle(Toxi::Rect.new 5, 5, width-5, height-5)
            @v.add_self @velocity
            @velocity.scale_self(1-@dampening)
        end

        def x
            @v.x
        end

        def y
            @v.y
        end
    end

    class Attractor
        attr_reader :v, :x, :y

        def initialize(x, y, r)
            @v = TVec2D.new x, y
            @r = r.to_f
        end

        def attract(node)
            dx = @v.x - node.x;
            dy = @v.y - node.y;
            d = @v.distance_to node.v

            return if d > @r

            s = d/@r
            f = (1 / (s**0.5) - 1) / @r

            node.v.add_self(dx*f, dy*f)
        end

        def update
            a = 10
            @v.add_self(rand(-a..a), rand(-a..a))
        end

        def x
            @v.x
        end

        def y
            @v.y
        end
    end

    class AttractionGrid
        def initialize(width, height, pg, scale)
            @width = width
            @height = height
            @pg = pg
            @scale = scale

            @spacing_y = 15
            @spacing_x = 10

            init_nodes
        end

        def init_nodes
            @g = (0..@height).step(@spacing_y).collect do |y|
                (0..@width).step(@spacing_x).collect do |x|
                    Node.new(x, y)
                end
            end
            @attractors = rand(6..9).times.collect do |i|
                Attractor.new(rand(0..@width), rand(0..@height), rand(150..300))
            end
        end

        def draw
            @attractors.each { |a| a.update }

            @pg.begin_draw

            @pg.background 255
            @pg.no_fill
            @pg.stroke_weight 1
            @pg.stroke 0

            @pg.scale(@scale)
            @g.each do |row|
                @pg.begin_shape
                row.each do |v|
                    @pg.vertex v.x, v.y
                    @attractors.each { |a| a.attract v }
                    v.update
                end
                @pg.end_shape
            end

            @pg.end_draw
            @pg
        end

    end
end

Attraction.new unless defined? $app
