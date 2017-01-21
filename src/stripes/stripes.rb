require 'jruby_art'
require 'jruby_art/app'
require 'toxiclibs'

require_relative '../_lib/pflib/pflib'
java_import Java::processing::core::PVector

class Stripes < PF::ProcessingApp
    attr_accessor :stripe_width, :offset, :show_points

    include_package 'controlP5'

    def settings
        size 500, 500, P2D
    end

    def setup
        super
        no_loop
        no_stroke
        texture_wrap REPEAT
        noise_detail 2, 0.45

        @stripe_width = 4
        @offset = @stripe_width
        @show_points = false

        @curves, @shapes = [], []

        build_texture
        build_curves
        build_shapes

        build_gui
    end

    def draw
        clear

        @offset = (frame_count/20.0) % 2*@stripe_width unless paused?

        # sloooooooow
        build_texture
        build_shapes.each do |shape|
            draw_points(shape) if @show_points
            shape(shape)
        end
    end

    def stripe_width=(w)
        @stripe_width = @offset = w
    end

    def draw_points(shape)
        (0...shape.get_vertex_count).each do |i|
            v = shape.get_vertex i
            push_style
            stroke 255, 0, 0
            ellipse v.x, v.y, 1, 1
            pop_style
        end
    end

    def build_texture
        pg = create_graphics 2*@stripe_width, 1
        pg.begin_draw
        pg.background 255
        pg.fill 0
        pg.no_stroke
        pg.rect(0, 0, @stripe_width, pg.height)
        pg.end_draw
        @texture = pg.get
    end

    def build_curves
        @curves = []

        offset = rand(height/10.0..height/4)
        @wave = PF::NoiseWave.new 0, 1/30.0, 100, offset

        @curves << [PVector.new(0, 0), PVector.new(width, 0)]

        until offset >= height - 30
            @curves << (0..width).step(1).collect do |x|
                PVector.new(x, @wave.update)
            end
            offset += rand(60..100)
            @wave.offset = offset
        end

        @curves << [PVector.new(0, height), PVector.new(width, height)]
        @curves
    end

    def build_shapes
        @curves.zip(@curves[1..-1])
            .reject { |c| c[1].nil? }
            .each_with_index { |c, i| @shapes << build_shape(c[0] + c[1].reverse, i.even? ? 0 : @offset) }
        @shapes
    end

    def build_shape(vertices, texture_offset)
        s = create_shape
        s.begin_shape
        s.texture(@texture)
        vertices.each { |v| s.vertex(v.x, v.y, v.x+texture_offset, v.y) }
        s.end_shape CLOSE
        s
    end

    def mouse_pressed
    end

    def key_pressed(e)
        case key
            when 'r'
                reset_seed true
                build_curves
            when '1', '2'
            else
                super
        end

        redraw
        sketch_title "#{paused? ? 'paused' : ''}"
    end

    def build_gui
        super do |cp|
            cp.add_button('points')
                .set_switch(true)
                .on_click do |e|
                    $app.show_points = !$app.show_points
                end

            clear_button = cp.get_controller('clear')
            clear_button.linebreak
            cp.add_slider('stripe_width')
                .set_value(@stripe_width)
                .set_range(1, 15.0)
                .set_height(60)
                .set_width(clear_button.get_width*2)

            #cp.add_slider('offset')
            #    .set_value(@offset)
            #    .set_range(0, 40.0)
            #    .set_height(60)
            #    .set_width(clear_button.get_width*2)
            #    .set_position(0, 19+60)
        end
    end
end

Stripes.new unless defined? $app
