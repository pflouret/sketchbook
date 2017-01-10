require 'jruby_art'
require 'jruby_art/app'
require 'toxiclibs'

require_relative '../_lib/pflib/pflib'

class PolyLines < Processing::App

    load_library :controlP5
    include_package 'controlP5'
    include PF::Gfx

    def settings
        size 800, 800
    end

    def setup
        sketch_title ''
        no_fill

        new_seed
        build_gui

        @gfx = Gfx::ToxiclibsSupport.new(self)
        @paused = false
    end

    def draw
        clear
        background 255, 0, 0, 100
        #s = createShape(TRIANGLE, [mouse_x, mouse_y-30, mouse_x-30, mouse_y+20, mouse_x+40, mouse_y+40])

        r = 400
        a = create_graphics r, r

        a.begin_draw
        a.push_matrix
        a.background 255, 0
        a.stroke_weight 1
        a.stroke 0, 255
        #a.translate a.width, 0
        #a.rotate 90.radians
        (0..a.height).step(6) do |y|
            a.line 0, y, a.width, y
        end
        a.pop_matrix
        a.end_draw

        s = create_shape
        s.begin_shape
        s.no_stroke
        #s.texture(load_image '/Users/pflouret/Pictures/926275_465051520278243_60274537_n.jpg')
        s.texture a.get
        s.vertex -130, -120, 130, 0
        s.vertex 0, 130, 0, 0
        s.vertex 140, 140, 0, 140
        s.end_shape CLOSE
        shape(s, mouse_x, mouse_y)
    end

    def draw1
        clear
        background 100

        r = 200
        a = create_graphics r, r
        b = create_graphics r, r

        a.begin_draw
        a.no_stroke
        a.background 255, 0
        a.fill 0, 0, 255, 255
        a.rect 50, 75, 100, 25
        a.rect 50, 125, 100, 25
        #(0..l1.height).step(4) do |y|
        #    l1.line 0, y, 200, y
        #end
        a.end_draw

        b.begin_draw
        b.no_stroke
        b.background 0
        b.fill 255
        b.ellipse 100, 100, 100, 100
        b.end_draw

        #l1.mask mask1
        #l2.mask mask2
        #a.blend b, 0, 0, 200, 200, mouse_x-width/2-r/2, mouse_y-width/2-r/2, 200, 200, SUBTRACT
        #a.blend b, 0, 0, 200, 200, 0, 0, 200, 200, MULTIPLY
        a.mask(b)
        a.load_pixels
        a.pixels.each_with_index do |c, i|
            a.pixels[i] = color(255, 0) if [red(c), green(c), blue(c)] == [255, 255, 255]
        end
        a.update_pixels


        #image(l1, width/2-r, height/2-r)
        image(a, width/2-r/2, height/2-r/2)
        #blend(b, 0, 0, r, r, width/2-r/2, height/2-r/2, r, r, DIFFERENCE)
        #image(b, width/2-r/2, height/2-r/2)
        #image(b, mouse_x-r/2, mouse_y-r/2)
        #image(pg2, width/2-r, height/2-r)
        no_loop

    end

    def clear
        background 255
    end

    def mouse_pressed
    end

    def key_pressed
        case key
            when 'p'
                if @paused then loop else no_loop end
                @paused = !@paused
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

        sketch_title "#{@paused ? '' : 'paused'}"
    end

    def new_seed
        @seed = Random.rand 99999
        @r = Random.new(@seed)
        noise_seed @seed
    end

    def build_gui
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

PolyLines.new unless defined? $app
