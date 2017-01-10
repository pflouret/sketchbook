require 'jruby_art'
require 'jruby_art/app'
require 'toxiclibs'

require_relative '../pflib'

class TVec2D
    attr_accessor :v0
end

class Attractor < Processing::App

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
        stroke 0, 150

        @gfx = Gfx::ToxiclibsSupport.new(self)
        @looping = true

        @spacing_y = 8
        @spacing_x = 1
        @g = (0..height).step(@spacing_y+1).collect do |y|
            (0..width).step(@spacing_x+1).collect do |x|
                v = TVec2D.new x, y
                v.v0 = v.copy
                v
            end
        end
        @attractors = []

        #no_loop
    end

    def draw
        clear

        @g.each do |row|
            begin_shape
            row.each do |v|
                @attractors.each do |a|
                    set_new_coords(a, v)
                end
                vertex v.x, v.y
            end
            end_shape
        end
    end

    def set_new_coords(attractor, v)
        d = attractor.distance_to v
        return if d < 2 #|| v.distance_to(v.v0) > @spacing
        v.interpolate_to_self attractor, 1/d**2
    end

    def clear
        background 255
    end

    def mouse_pressed
        @attractors << TVec2D.new(mouse_x, mouse_y)
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
end

Attractor.new unless defined? $app
