require 'jruby_art'
require 'jruby_art/app'
require 'toxiclibs'

require_relative '../pflib'

class A < Processing::App

    def settings
        size 800, 400
    end

    def setup
        sketch_title ''
        no_fill
        no_loop

        @seed = Random.rand 99999
        @r = Random.new(@seed)
        @n = Toxi::PerlinNoise.new
        @n.noise_seed @seed
        noise_seed @seed

        @looping = true
        @gfx = Gfx::ToxiclibsSupport.new(self)

        @octaves = 6
        @falloff = 0.5
    end

    def draw
        clear
        load_pixels
        noise_detail @octaves, @falloff
        @n.noise_detail @octaves, @falloff
        (0...height).each do |y|
            (0...width).each do |x|
                if x < width / 2
                    nx = 2*x.to_f/width
                    ny = 2*y.to_f/height
                    n = noise(nx, ny)
                else
                    nx = 2*(x.to_f-width/2)/width
                    ny = 2*y.to_f / height
                    n = @n.noise(nx, ny)
                end
                pixels[y*width + x] = color(n*255)
            end
        end
        update_pixels
    end

    def clear
        background 255
    end

    def mouse_pressed
    end

    def key_pressed
        case key
            when 'p'
                if @looping then no_loop else loop end
                @looping = !@looping
            when 'z', 'x'
                @octaves = @octaves + (key == 'z' ? -1 : 1)
            when 'a', 's'
                @falloff = @falloff + (key == 'a' ? -1 : 1)*0.05
            when 'c'
                clear
            when 'r'
                @seed = Random.rand 99999
                @r = Random.new(@seed)
                @n = Toxi::PerlinNoise.new
                @n.noise_seed @seed
                noise_seed @seed
            when 'S'
                screenshot
                screenshot @r.seed
            else
        end
        sketch_title "#{@octaves} #{@falloff}"
        redraw
    end

end

A.new unless defined? $app
