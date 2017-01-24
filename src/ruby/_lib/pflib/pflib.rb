require 'fileutils'
require 'pathname'

require 'jruby_art'
require 'jruby_art/app'
require 'toxiclibs'

module PF
    class ProcessingApp < Processing::App
        attr_reader :r

        load_library :controlP5
        include_package 'controlP5'

        def initialize
            super

            reset_seed true

            @gfx = Gfx::ToxiclibsSupport.new self
            @paused = false
            @bg = 255
            @cp = nil
            @control_event_queue = Queue.new
            @redraw_on_event = true
        end

        def settings
            size 1080, 1080
        end

        def setup
            surface.set_resizable true
            register_method 'pre', ControlVarProxy.new
            sketch_title ''
            no_fill
        end

        def clear
            background @bg
        end

        def reset_seed(new_seed=false)
            @seed = Random.rand 999999999 if new_seed
            @r = Random.new(@seed)
            random_seed @seed
            noise_seed @seed
        end

        def rand(*args)
            @r.rand *args
        end

        def key_pressed(e=nil)
            case key
                when 'p'
                    toggle_loop
                when 'c'
                    clear
                when 'r'
                    reset_seed true
                when 'S'
                    @control_event_queue << Class.new do
                        def self.name
                            'screenshot'
                        end
                    end
                else
                    return false
            end

            true
        end

        def screenshot
            name = Pathname.new(SKETCH_ROOT).basename
            dir = Pathname.new('~/code/sketchbook/screenshots').expand_path.join(name)
            FileUtils.mkdir_p dir

            ts = Time.now.strftime '%Y-%m-%d_%H%M%S'
            seed = '_s%05i' % @seed
            path = dir.join "#{ts}#{seed}_f#####.png"

            save_frame path.to_s
        end

        def build_gui
            # FIXME: Move this to ControlWindow.

            w = ControlWindow.new self
            w.surface.pause_thread

            cp = ControlP5.new w
            cp.set_auto_spacing 1, 1

            cp.begin 0, 0
            b = cp.add_button('_toggle_loop')
                .set_value(0)
                .set_position(0, 0)
                .set_switch(true)
                .set_label(paused? ? 'resume' : 'pause')
                .update_size
                .on_click do |e|
                    $app.toggle_loop
                    button = e.get_controller
                    button.set_label(paused? ? 'resume' : 'pause')
                    button.update_size
                end
            paused? ? b.set_on : b.set_off


            cp.add_button('clear')
                .set_value(0)
                .update_size

            yield(cp, w) if block_given?

            cp.add_listener(ControlVarProxy.new)

            cp.end
            @cp = cp

            w.surface.resume_thread
        end

        def paused?
            @paused
        end

        def loop
            super
            @paused = false
        end

        def no_loop
            super
            @paused = true
        end

        def toggle_loop
            paused? ? loop : no_loop
        end

        def control_event(e)
            @control_event_queue << e
            redraw if @redraw_on_event
        end

        def process_control_events
            until @control_event_queue.empty?
                e = @control_event_queue.deq(false)
                f = self.respond_to?("#{e.name}=") ? self.public_method("#{e.name}=") : nil
                w = self.respond_to?(e.name) ? self.public_method(e.name) : nil
                m = f || w
                return if m.nil?
                args = m.arity == 0 ? [] : [e.value]
                m.call(*args)
            end
        end

        def method_missing(m, *args, **kwargs, &block)
            # Maybe it's defined in PGraphics.
            begin
                g.send(m, *args, **kwargs, &block)
            rescue
            end
        end
    end

    class ControlVarProxy
        java_implements Java::controlP5.ControlListener
        java_signature 'void controlEvent(controlP5.ControlEvent)'
        def controlEvent(e)
            $app.control_event e
        end

        def pre
            $app.process_control_events
        end
        become_java!
    end

    class ControlWindow < Processing::App
        def initialize(parent, width=400, height=600)
            @parent = parent
            @w, @h = width, height
            super()
            $app = @parent
        end

        def settings
            size @w, @h
        end

        def setup
            background 0
            sketch_title ''
            surface.set_location 7, 30
            surface.set_resizable true
        end

        def draw
            background 0
        end
    end

    java_import Java::toxi::math::waves::AbstractWave
    class NoiseWave < AbstractWave
        attr_accessor :resolution

        def initialize(phase, freq, amp, offset)#, resolution)
            super(phase, freq, amp, offset)
            #@resolution = resolution
            @phase = phase
            @climb = 0
            @dir = $app.rand < 0.5 ? -1 : 1
        end

        def update
            n = $app.map1d($app.noise(@phase*frequency), 0..1, -amp/2.0..amp/2.0)
            y = n + offset + @climb

            if $app.rand < 0.005 || y <= 0 || y >= $app.height || @climb > $app.rand(10..40)
                @dir = -@dir
                y = $app.constrain(y, 15, $app.height-15)
            end
            @climb += @dir*0.2
            @phase += 0.8
            y
        end
    end
end

java_import 'processing.core.PGraphics'
class PGraphics
    def humanline(x0, y0, x1, y1, squiggle_factor=0.5, r=nil)
        r ||= $app.r

        d = Processing::App.dist(x0, y0, x1, y1)
        tf = 2.0

        s = squiggle_factor
        squiggle_range = if d <= 150 then (-0.5*s..0.5*s) else d <= 400 ? (-1.0*s..1.0*s) : (-2.0*s..2.0*s) end
        step = if d <= 200 then 0.5 else d <= 400 ? 0.3 : 0.2 end

        points = (0..tf).step(step).collect do |t|
            Vec2D.new f(x0, x1, t/tf, squiggle_range, r), f(y0, y1, t/tf, squiggle_range, r)
        end
        points.unshift points[0]
        points.push points[-1]

        self.begin_shape
        points.each do |v|
            self.curve_vertex v.x, v.y
            yield v if block_given?
        end
        self.end_shape

        points
    end

    def humanline_fill(squiggle_factor: 0.4, spacing: 4, scale: 1.45, squish: true, style: nil, r: nil)
        r ||= $app.r
        push_matrix
        push_style

        style(style) unless style.nil?

        w2, h2 = width/2, height/2
        translate w2, h2
        rotate -r.rand(0..PI)
        scale(scale)

        offset = 0

        y = -h2
        until y > h2
            yield if block_given?

            humanline -w2, y-offset, w2, y-offset, squiggle_factor, r

            offset = squish ? calc_offset(offset, spacing, r) : 0
            y += spacing - offset
        end

        pop_style
        pop_matrix
    end

    private

    def f(c0, c1, tau, squiggle_range, r)
        squiggle = r.rand(squiggle_range)
        c0 + (c0 - c1)*(15*((tau)**4) - 6*((tau)**5) - 10*((tau)**3)) + squiggle
    end

    def calc_offset(prev_offset, spacing, r)
        offset = prev_offset
        if r.rand < 0.15 && offset == 0
            offset = r.rand(0..spacing/2.0)
            offset -= r.rand(0..offset/1.7)
        elsif r.rand < 0.05 && offset != 0
            offset = 0
        end
        offset
    end
end


class A < PF::ProcessingApp
    def setup
        super
        build_gui
    end

    def draw
        ellipse rand(0..width), rand(0..height), 5, 5
    end
end

#A.new unless defined? $app
