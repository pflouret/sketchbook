require 'jruby_art'
require 'jruby_art/app'
require 'toxiclibs'

require_relative '../pflib'

class MeshCrap < Processing::App

    load_library :controlP5
    include_package 'controlP5'

    def settings
        size 800, 800, P3D
    end

    def setup
        sketch_title ''
        no_fill
        #no_loop

        new_seed
        #build_gui

        @gfx = Gfx::ToxiclibsSupport.new(self)
        @looping = true

        @n = @m = 30
        @step = 10
        @scale = 12

        stroke_weight 0.1
        stroke 0, 30

        @xa = @ya = 0
        #no_loop
    end

    def draw
        clear

        #rotate_z(PI/2)
        #translate((width - @step*@n)/2, (height - @step*@m)/2)
        translate(width/2, height/2)
        scale(@scale)
        rotate_x(@xa.radians)
        @xa += 0.5
        @ya += 0.5
        rotate_y(@ya.radians)
        rotate_z(@ya.radians)
        #rotate_x(map1d(mouse_x, 0..height, 0.0..2*PI))
        #rotate_y(map1d(mouse_y, 0..height, 0.0..2*PI))
        #rotate_z(map1d(mouse_y, 0..height, 0.0..2*PI))

        noise_detail 4, 0.81
        noise_scale = 10.0
        (-@m/2..@m/2).each do |y|
            #begin_shape(QUAD_STRIP)
            begin_shape(TRIANGLE_STRIP)
            (-@n/2..@n/2).each do |x|
                z = noise(x/noise_scale, y/noise_scale)#sin(sqrt(x*x + y*y))
                z = map1d(z, 0..1, -3..3)
                vertex x, y, z

                z = noise(x/noise_scale, (y+1)/noise_scale)#sin(sqrt(x*x + y*y))
                z = map1d(z, 0..1, -3..3)
                #z = sin(sqrt(x*x + (y+1)*(y+1)))
                vertex x, y+1, z
            end
            end_shape
        end

    end

    def clear
        background 255
    end

    def mouse_moved
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
            when 'r'
                new_seed
            when 'S'
                screenshot @seed
            else
        end

        sketch_title "#{@looping ? '' : 'paused'}"
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


        @cp.add_slider('step')
            .set_value(50)
            .set_range(0, 100)
            .set_height(19)
            .update_size
            .on_change { |e| @step = e.get_controller.get_value }

        @cp.end
    end
end

MeshCrap.new unless defined? $app
