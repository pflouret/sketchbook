require 'jruby_art'
require 'jruby_art/app'
require 'toxiclibs'

require_relative '../_lib/pflib/pflib'

class Verlet < PF::ProcessingApp
    attr_accessor :circle

    #include Physics
    load_library :controlP5
    include_package 'controlP5'
    include_package 'toxi.geom'
    include_package 'toxi.physics2d'
    include_package 'toxi.physics2d.constraints'

    def settings
        size 500, 500
    end

    def setup
        super

        no_loop

        @circle = Toxi::Circle.new width/2, height/2, 300

        @ph = VerletPhysics2D.new
        @ph.set_index(PointQuadtree.new(0, 0, width, height))

        # @ph.add_constraint(Class.new do
        #    include ParticleConstraint2D
        #    def apply(p)
        #        unless $app.circle.contains_point p
        #            p.set($app.circle.add(p.sub($app.circle).normalize_to($app.circle.get_radius)));
        #        end
        #    end
        # end.new)
        #@ph.add_constraint(Class.new do
        #    include ParticleConstraint2D
        #    def apply(p)
        #        if $app.circle.contains_point p
        #            tan = $app.circle.get_tangent_points(p)
        #            p.set(p.distance_to(tan[0]) < p.distance_to(tan[1]) ? tan[0] : tan[1]) if tan
        #        end
        #    end
        #end.new)
    end

    def draw
        clear

        @ph.update


        fill 0, 255, 0, 5
        stroke_width 2
        stroke 0, 255, 0, 20
        ellipse @circle.x, @circle.y, @circle.get_radius, @circle.get_radius

        within = @ph.get_index.items_within_radius(TVec2D.new(@circle.x, @circle.y), @circle.get_radius/2, [])
        @ph.particles.each do |p|
            if within.include? p
                fill 0, 255, 0
                stroke_width 4
                stroke 0, 255, 0, 20
            else
                assert (@ph.particles - within).include? p
                fill 255, 0, 0
                stroke_width 4
                stroke 0, 20
            end
            ellipse p.x, p.y, 6, 6
        end

        sketch_title "#{frame_rate.round.to_s}fps"
        add_particles
        loop
    end

    def add_particles
        1.times do
            p = VerletParticle2D.new(rand(0..width), rand(0..height))
            @ph.add_particle(p)
            #@ph.particles.each do |pp|
            #    @ph.add_spring(LineSpring.new(p, pp))
            #end
        end
        redraw
    end

    def mouse_pressed
        add_particles
    end

    def key_pressed(e)
        case key
            when '1', '2'
                add_particles
            when 'q', 'w'
                @circle.set_radius(@circle.get_radius + 1)
            when 'a', 's'
            when 'z', 'x'
            else
                super
        end

        sketch_title "#{paused? ? 'paused' : ''}"
        redraw
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

Verlet.new unless defined? $app
