require 'jruby_art'
require 'jruby_art/app'
require 'toxiclibs'

require_relative '../pflib'

class Lissajous < Processing::App

    load_library :controlP5
    include_package 'controlP5'

    def settings
        size 900, 900
    end

    def setup
        sketch_title ''
        no_fill
        no_loop

        new_seed

        @gfx = Gfx::ToxiclibsSupport.new(self)
        @looping = true
        @a = width / 2 - 150
        @b = height / 2 - 150
        @freq_a = 5
        @freq_b = 4
        @phi = PI / 2

        stroke_weight 1
        stroke 0, 50

        build_gui
    end

    def draw
        clear

        push_matrix


        translate(width/2, height/2)
        #begin_shape
        (0..500).step(0.01) do |t|
            point(@a*sin(@freq_a*t + @phi), @b*sin(@freq_b*t))
        end
        #end_shape


        pop_matrix
    end

    def clear
        background 255
    end

    def mouse_pressed
        redraw
    end

    def mouse_moved
        redraw
    end

    def mouse_dragged
        redraw
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
        #    .set_value(@a)
        #    .set_range(0, 500)
        #    .set_height(19)
        #    .update_size
        #    .on_change { |e| @a = e.get_controller.get_value }

        #@cp.add_slider('b')
        #    .set_value(@b)
        #    .set_range(0, 500)
        #    .set_height(19)
        #    .update_size
        #    .on_change { |e| @b = e.get_controller.get_value }

        freq_range = (1..35)
        @cp.add_slider('freq_a')
            .set_value(@freq_a)
            .set_range(freq_range.min, freq_range.max)
            .set_number_of_tick_marks(freq_range.size)
            .snap_to_tick_marks(true)
            .set_height(19)
            .update_size
            .on_change { |e| @freq_a = e.get_controller.get_value }

        @cp.add_slider('freq_b')
            .set_value(@freq_b)
            .set_range(freq_range.min, freq_range.max)
            .set_height(19)
            .update_size
            .on_change { |e| @freq_b = e.get_controller.get_value }
            .set_number_of_tick_marks(freq_range.size)
            .snap_to_tick_marks(true)

        @cp.add_slider('phi')
            .set_value(0.0)
            .set_range(0.0, 2*PI)
            .set_number_of_tick_marks(40)
            .snap_to_tick_marks(true)
            .set_height(19)
            .update_size
            .on_change { |e| @phi = e.get_controller.get_value }

        @cp.end
    end
end

Lissajous.new unless defined? $app

