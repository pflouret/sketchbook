require 'jruby_art'
require 'jruby_art/app'
require 'toxiclibs'

require_relative '../pflib'

class A < Processing::App

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

        strokeWeight 0.75
        stroke 0, 100
        fill 0, 5


        @r = 200

        no_loop
    end

    def draw
        clear

        push_matrix
        translate width/2, height/2
        rotate_x(map1d(mouse_y, 0..width, 0..2*PI))
        rotate_y(map1d(mouse_x, 0..width, 0..2*PI))
        s = Toxi::Sphere.new @r
        #s.scale 100
        @gfx.sphere s, 20
        pop_matrix
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
end

A.new unless defined? $app
