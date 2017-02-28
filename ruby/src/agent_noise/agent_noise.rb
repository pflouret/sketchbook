require 'jruby_art'
require 'jruby_art/app'

require_relative '../pflib'

class AgentNoise < Processing::App

    def settings
        size 1200, 800
    end

    def setup
        sketch_title ''
        no_fill
        #no_loop
        stroke_weight 0.3
        stroke 0, 150
        @bg = 255

        new_seed
        @looping = true

        @octaves = 3
        @falloff = 0.5
        @speed = 1300
        @max_step = 0.9
        @scale = 500

        @y = 0

        @agents = 20.times.collect { Vec2D.new(@r.rand(0...width), @r.rand(0...height)) }

        clear
        key_pressed
    end

    def new_seed
        @seed = Random.rand 99999
        @r = Random.new(@seed)
        noise_seed @seed
    end

    def draw
        @speed.times { draw_agents_2d }
    end

    def draw_agents_1d
        noise_detail @octaves, @falloff

        @agents.each do |a|
            unless (0..width).include?(a.x)
                r = @r.rand(0..@max_step)
                a.x = 0
                a.y = @y+r#mouseY#@r.rand(0.0...height)
                @y += r
            end

            point a.x, a.y

            #nx = a.x.to_f / width
            nx = a.x.to_f / @scale
            move_agent_1d_1(a, noise(nx))
        end
    end

    def move_agent_1d_1(a, n)
        a.x += 0.2#@r.rand(0.1..0.4)
        a.y += map1d(n, 0..1, -0.5..0.5)
        #a.y += @r.rand(-0.2..0.2)
    end

    def draw_agents_2d
        noise_detail @octaves, @falloff

        @agents.each do |a|
            unless (0..width).include?(a.x) and (0..height).include?(a.y)
                a.x = @r.rand(0...width)
                a.y = @r.rand(0...height)
            end

            point a.x, a.y

            nx = a.x.to_f / width
            ny = a.y.to_f / height
            move_agent_2d_2(a, noise(nx, ny))
        end
    end

    def move_agent_2d_1(a, n)
        angle = map1d(n, 0..1, -2*PI..0)
        dir = Vec2D.from_angle angle
        a.x += dir.x
        a.y += dir.y
    end

    def move_agent_2d_2(a, n)
        angle = map1d(n, 0..1, -2*PI..0)
        dir = Vec2D.from_angle angle.abs
        a.x += dir.x
        a.y += dir.y
    end

    def move_agent_2d_3(a, n)
        angle = map1d(n, 0..1, -2*PI..0)
        dir = Vec2D.from_angle angle
        a.x -= 2*dir.x
        a.y += dir.y
    end

    def clear
        background @bg
        @y = 0
    end

    def key_pressed
        case key
            when 'p'
                if @looping then no_loop else loop end
                @looping = !@looping
            when 'z', 'x'
                @octaves = @octaves + (key == 'z' ? -1 : 1)
            when 'a', 's'
                @falloff = @falloff + (key == 'a' ? -1 : 1)*0.01
            when '1', '2'
                @speed = @speed + (key == '1' ? -1 : 1)*10
            when '3', '4'
                @max_step = @max_step + (key == '3' ? -1 : 1)*0.1
            when '5', '6'
                @scale = @scale + (key == '5' ? -1 : 1)*1
            when 'c'
                clear
            when 'r'
                new_seed
            when 'S'
                screenshot @seed
            else
        end

        sketch_title "#{@octaves} #{@falloff.round(2)} #{@speed} #{@max_step.round(2)} #{@scale} #{@looping ? '' : 'paused'}"
    end

end

AgentNoise.new unless defined? $app
