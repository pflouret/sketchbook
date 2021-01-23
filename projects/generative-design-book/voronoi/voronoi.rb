require 'jruby_art'
require 'jruby_art/app'
require 'toxiclibs'

require_relative '../pflib'

class Voronoi < Processing::App
    #attr_reader :increment, :z_increment

    def setup
        sketch_title 'voronoi'
        #no_loop
        no_fill
        #frame_rate 10

        @r = Random.new
        @gfx = Gfx::ToxiclibsSupport.new(self)
        @clipper = Toxi::SutherlandHodgemanClipper.new(Toxi::Rect.new(0, 0, width, height))
        @v = Toxi::Voronoi.new
        @v.add_points (1...100).collect { TVec2D.new(@r.rand(width), @r.rand(height)) }
    end

    def draw
        background 255

        stroke 0, 100
        @v.get_regions.each do|r|
            stroke_weight 2
            @gfx.polygon2D(r)
            stroke_weight 6
            r.vertices.each { |v| point v.x, v.y }
        end

        stroke 255, 0, 0, 40
        @v.get_triangles.each do |t|
            stroke_weight 2
            @gfx.triangle t
            stroke_weight 6
            t.get_vertex_array.each { |v| point v.x, v.y }
        end

        lloyd
    end

    def lloyd
        @v = new_voronoi(@v.get_regions.collect { |r| @clipper.clip_polygon(r).get_centroid })
    end

    def new_voronoi(points)
        v = Toxi::Voronoi.new
        v.add_points points
        v
    end

    def mouse_pressed
        #puts (@v.get_sites.collect { |p| dist(p.x, p.y, mouse_x, mouse_y) < 100 ? p.add_self(p) : p })
        @v = new_voronoi(@v.get_sites.collect do |p|
            if dist(p.x, p.y, mouse_x, mouse_y) < 150
                #p.jitter(400).constrain(Toxi::Rect.new(0, 0, width, height))
                p.scale(3).constrain(Toxi::Rect.new(50, 50, width-50, height-50))
            else
                p
            end
        end)
    end

    def key_pressed
        case key
            when 'l'
                lloyd
            when 'S'
                screenshot
                screenshot @r.seed
            else
        end
        redraw
    end

    def settings
        size 800, 800
    end
end

Voronoi.new unless defined? $app

