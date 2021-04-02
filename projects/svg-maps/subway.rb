require 'json'
require 'mustache'
require 'ostruct'
require 'pry'
require 'pry-byebug'
require 'set'

ZOOM = 19
SAVE_REF = false

def run(data_path)
  data = open(data_path) { |f| JSON.parse(f.read, symbolize_names: true) }[:elements]
    .map(&OpenStruct.method(:new))

  $nodes = data
    .select { |d| d.type == 'node' }
    .reduce({}) { |m, d| m.merge!(d.id => d) }

  $ways = data
    .select { |d| d.type == 'way' }
    .reduce({}) { |m, d| m.merge!(d.id => d) }

  rels = data.select { |d| d.type == 'relation' }
  rels_by_ref = rels.group_by { |r| r.tags[:ref] }
  rels_by_ends = (rels_by_ref.delete(nil) || [])
    .group_by { |r| [r.tags[:from], r.tags[:to]].to_set }

  basename = File.basename(data_path, '.json')

  save("#{basename}-ref.svg", rels.map { build_draw_objs(_1) }.flatten(1)) if SAVE_REF

  draw_objs =
    rels_by_ref.map { |ref, rels| build_draw_objs(rels.first) }.flatten(1) +
    rels_by_ends.map { |ends, rels| build_draw_objs(rels.first) }.flatten(1)

  save("#{basename}.svg", draw_objs)
end

def build_draw_objs(rel)
  lines = rel.members
    .select { |e| e[:type] == 'way' && e[:role].empty? }
    .map { |e| $ways[e[:ref]] }
    .map { |w| w.nodes.map { |n| $nodes[n] } }

  stops = rel.members
    .select { |e| e[:type] == 'node' && e[:role] == 'stop' }
    .map { |e| $nodes[e[:ref]] }

  platforms = rel.members
    .select { |e| e[:type] == 'way' && e[:role] == 'platform' }
    .map { |e| $ways[e[:ref]] }
    .map { |w| w.nodes.map { |n| $nodes[n] } }

  OpenStruct.new(
    rel: rel,
    lines: lines,
    stops: stops,
    platforms: platforms
  )
end

class Subway < Mustache
  self.template_path = __dir__
end

def save(filename, draw_objs)
  draw_objs.each do |o|
    o.lines = o.lines.map do |path|
      path.map { |n| project(n.lat, n.lon, ZOOM) }
    end
    o.stops = o.stops.map do |n|
      project(n.lat, n.lon, ZOOM)
    end
  end

  points = draw_objs.map(&:lines).flatten(2)
  xx, yy = points.map { |p| p[0] }, points.map { |p| p[1] }
  min_x, min_y = xx.min, yy.min

  t = Subway.new
  t[:groups] =
    draw_objs.map do |o|
      color = o.rel.tags[:colour] || 'black'
      ref = o.rel.tags[:ref].encode(xml: :text)
      lines = o.lines.map do |path|
        s = path.map { |p| "#{p[0]-min_x} #{p[1]-min_y}" }.join(' ')
        %(<path d="M#{s}"/>)
      end.join("\n    ")
      stops = o.stops.map do |p|
        %(<circle cx="#{p[0]-min_x}" cy="#{p[1]-min_y}" r="1"/>)
      end.join("\n    ")
      <<~END
        <g inkscape:label="#{color}: #{ref}" inkscape:groupmode="layer" stroke="#{color}" stroke-width="1" fill="none">
            #{lines}
            #{stops}
          </g>
      END
    end
    .join("\n  ")

  open(filename, 'w') { |f| f << t.render }
end

def project(lat_deg, lng_deg, zoom)
  lat_rad = lat_deg/180 * Math::PI
  n = 2.0 ** zoom
  x = ((lng_deg + 180.0) / 360.0 * n)
  y = ((1.0 - Math::log(Math::tan(lat_rad) + (1 / Math::cos(lat_rad))) / Math::PI) / 2.0 * n)
  [x, y]
end

ARGV.each(&method(:run))
