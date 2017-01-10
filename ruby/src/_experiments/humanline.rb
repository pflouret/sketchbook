require 'jruby_art'
require 'jruby_art/app'

require_relative '../_lib/pflib/pflib'

class HumanLine < PF::ProcessingApp

    load_libraries :controlP5, :AULib
    include_package 'controlP5'
    include_package 'AULib'

    def setup
        super

        @squish = true
        @squiggle_factor = 0
        @spacing = 3
        @scale = 1.7
        #no_loop
    end

    def draw
        clear
        background 0, 100, 0
        reset_seed

        stroke_weight 1
        stroke 0, 130

        no_fill
        stroke_weight 0.5
        stroke 0, 140
        #stroke rand(255), rand(255), rand(255), rand(150..255)

        color_mode HSB, 360, 100, 100
        @hue ||= 0
        humanline_fill(squiggle_factor: @squiggle_factor, spacing: @spacing,
                       scale: @scale, squish: @squish, style: g.get_style) do
            r = (frame_count%360).radians
            stroke(@hue%360, map1d(sin(r), -1..1, 0..100), 100)
        end
        @hue += 1

    end

    def key_pressed(e)
        case key
            when '1', '2'
                @spacing = max(0.5, @spacing + (key == '1' ? -1 : 1)*0.5)
            when '3', '4'
                @squiggle_factor = max(0, @squiggle_factor + (key == '3' ? -1 : 1)*0.1)
            when '5', '6'
                @scale = @scale + (key == '5' ? -1 : 1)*0.05
            when 't'
                @squish = !@squish
            else
                super
        end

        sketch_title "#{@spacing.round(1)} #{@squiggle_factor.round(1)} #{@scale.round(2)} #{@paused}"

        redraw
    end

    def draw2
        clear
        stroke_weight 2
        stroke 0, 130
        stroke 0, 0

        v0 = Vec2D.new(400, 400)
        v1 = Vec2D.new(mouse_x, mouse_y)
        knots = points.collect { |v| [v.x, v.y].to_java(:float) }.to_java('[F')
        curve = AUCurve.new knots, 2, false

        begin_shape
        points.each do |v|
            curve_vertex v.x, v.y
        end
        end_shape

        stroke_weight 0.5
        stroke 0
        d = dist(400, 400, mouse_x, mouse_y)*2
        (0..d).each do |i|
            t = norm(i, 0, d)
            stroke rand(0..30)
            #ellipse(curve.get_x(t), curve.get_y(t), 1, 1)
            #point(curve.get_x(t), curve.get_y(t))
        end

        filter(BLUR, mouse_y/500.0)

        no_loop
    end


end

HumanLine.new unless defined? $app
