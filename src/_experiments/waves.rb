require 'jruby_art'
require 'jruby_art/app'
require 'toxiclibs'

require_relative '../_lib/pflib/pflib'
java_import Java::toxi::math::waves::SineWave
java_import Java::toxi::math::waves::FMSineWave
java_import Java::toxi::math::waves::FMTriangleWave
java_import Java::processing::core::PVector

class Waves < PF::ProcessingApp
    attr_accessor :ffreq, :foff, :fphase, :famp, :afreq, :aoff, :aphase, :aamp

    include_package 'controlP5'

    def settings
        size 1080, 600, P2D
    end

    def setup
        super
        #no_loop
        stroke 0, 200
        stroke_weight 2

        @curves, @shapes = [], []

        @n = 2


        @fphase = rand(0..2*PI)
        @aphase = 0
        @ffreq = rand(0..1)
        @afreq = rand(0..0.5)#2*PI / width
        @famp = @aamp = 10
        @foff = 0
        @aoff = 0

        update_waves

        @rgb = [255, 0, 0]
        @fphase = 1 # step
        @ffreq = 2 # speed
        @famp = 70
        @foff = 2 # detail
        @aphase = 0.5 # falloff
        @afreq = 0.005

        noise_detail @foff, @aphase
        @noise = (0..width).step(0.8).collect { |i| map1d(noise(i.to_f/@famp), 0..1, -100..100)+height/2 }
        @colors = @noise.length.times.collect { [0,0,0] }
        @i = width.to_f
        build_gui

        @dir = -1
        @offset = 0
    end

    def draw
        clear

        noise_detail @foff, @aphase

        begin_shape
        @noise.each_with_index do |y, i|
            stroke *@colors[i], 200
            vertex i, y
        end
        end_shape

        #(0..width).each { |x| vertex x, height/2.0+map1d(@noise[x], 0..1, -100..100)}
        @ffreq.to_i.times do |i|
            y = map1d(noise(@i/@famp), 0..1, -100..100) + height/2 + @offset

            if rand < @afreq || y <= 0 || y >= height || @offset > rand(15..50)
                @dir = -@dir
                y = constrain(y, 1, height-1)
            end
            @offset += @dir*0.3

            @noise.shift
            @noise.push(y)
            @colors.shift
            @colors.push(@rgb.dup)
            @i += @fphase
        end
        #p @noise
    end

    def draw2
        clear

        stroke 0, 20
        push_style
        line 0, height/2, width, height/2
        line width/2, 0, width/2, height
        pop_style

        @a.push; @b.push
        begin_shape
        (0..width).step(10) do |x|
            vertex(x, @a.update)
        end
        end_shape
        @a.pop; @b.pop

        @a.push; @b.push
        begin_shape
        (0..width).step(10) do |x|
            vertex(x, @b.update)
        end
        end_shape
        @a.pop; @b.pop

        stroke 255, 0, 0, 200
        @a.push; @b.push; @c.push
        begin_shape
        (0..width).step(5) do |x|
            vertex(x, @a.update + @b.update + @c.update)
        end
        end_shape
        @a.pop; @b.pop; @c.pop

        if rand(1.0) < 0.01
            @b.frequency = rand(0..0.2)
            @a.offset = rand(50..100)
        end

        av = @a.update; @b.update; @c.update
        #@c.offset = av
    end

    def update_waves
        @a = SineWave.new @fphase, @ffreq, @famp, @foff
        @b = SineWave.new @aphase, @afreq, @aamp, @aoff
        @c = FMTriangleWave.new 0, rand(0..0.3), 70, 100
    end

    def draw1
        clear

        push_style
        stroke 0, 30
        line 0, height/2, width, height/2
        line width/2, 0, width/2, height
        pop_style

        begin_shape
        push_matrix
        scale((width-20)/width)
        translate 10, 10
        @wave.push
        @fmod.push
        (0..width).step(10) do |x|
            vertex(x, @wave.update)
        end
        @wave.pop
        @fmod.pop
        @wave.update
        @fmod.update
        pop_matrix
        end_shape
    end

    def update_waves1
        @fmod = SineWave.new @fphase, @ffreq, @famp, @foff
        @amod = SineWave.new @aphase, @afreq, @aamp, @aoff
        #@wave = FMSineWave.new 0, 2*PI/width, height/4, height/2, @fmod
        @wave = AMFMSineWave.new 0, 2*PI/width, height/2, @fmod, @amod
    end

    def process_control_events
        #update_waves unless @control_event_queue.empty?
        unless @control_event_queue.empty?
            @rgb.push(@rgb.shift)
        end
        super
    end

    def key_pressed
        redraw
    end

    def build_gui
        super do |cp|
            h = 60
            cp.add_slider('fphase')
                .set_value(@fphase)
                .set_range(0, 3)
                .set_height(40)
                .set_width(370)
                .set_position(0, 20)

            cp.add_slider('ffreq')
                .set_value(@ffreq)
                .set_range(0, 20)
                .set_height(60)
                .set_width(370)
                .set_position(0, h+1)

            cp.add_slider('famp')
                .set_value(@famp)
                .set_range(0, 90)
                .set_height(60)
                .set_width(370)
                .set_position(0, 2*h+2)

            cp.add_slider('foff')
                .set_value(@foff)
                .set_range(0, 8)
                .set_height(60)
                .set_width(370)
                .set_position(0, 3*h+3)
                #.set_number_of_tick_marks(8)
                #.snap_to_tick_marks(true)

            cp.add_slider('aphase')
                .set_value(@aphase)
                .set_range(0, 1)
                .set_height(60)
                .set_width(370)
                .set_position(0, 4*h+4)

            cp.add_slider('afreq')
                .set_value(@afreq)
                .set_range(0, 0.2)
                .set_height(60)
                .set_width(370)
                .set_position(0, 5*h+5)

            cp.add_slider('aamp')
                .set_value(@aamp)
                .set_range(0, 100)
                .set_height(60)
                .set_width(370)
                .set_position(0, 6*h+6)

            cp.add_slider('aoff')
                .set_value(@aoff)
                .set_range(0, 300.0)
                .set_height(60)
                .set_width(370)
                .set_position(0, 7*h+7)
            #cp.add_slider('offset')
            #    .set_value(@offset)
            #    .set_range(0, 40.0)
            #    .set_height(60)
            #    .set_width(clear_button.get_width*2)
            #    .set_position(0, 19+60)
        end
    end
end

Waves.new unless defined? $app
