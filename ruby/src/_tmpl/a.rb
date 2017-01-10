require 'jruby_art'
require 'jruby_art/app'
require 'toxiclibs'

require_relative '../_lib/pflib/pflib'

class A < PF::ProcessingApp

    load_library :controlP5
    include_package 'controlP5'

    def setup
        super
    end

    def draw
        clear

        push_matrix
        translate width/2, height/2
        rotate((frame_count % 360).radians)
        strokeWeight 10
        triangle(-50, 50, 50, 50, 0, -sqrt(3)*100/4)
        pop_matrix
    end

    def mouse_pressed
    end

    def key_pressed(e)
        case key
            when '1', '2'
            when 'q', 'w'
            when 'a', 's'
            when 'z', 'x'
            else
                super
        end

        sketch_title "#{paused? ? 'paused' : ''}"
    end

    def build_gui
        super do |cp|
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
end

A.new unless defined? $app
