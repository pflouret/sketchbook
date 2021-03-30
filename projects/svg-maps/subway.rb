require 'json'
require 'ostruct'
require 'pry'
require 'pry-byebug'
require 'set'
require 'mustache'

ZOOM = 19
OUT_FILENAME = 'out.svg'

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

  stops = data
    .select { |n| n.type == 'node' }
    .reject { |n| n.tags&.dig(:name).nil? }

  names = rels_by_ref.map { [_1, _2.map { |r| r.tags[:name] } ] }.to_h
  #binding.pry

  if true
    node_paths = rels.map do |rel|
      get_node_paths(rel)
    end.flatten(1)
  else
    node_paths = rels_by_ref.map do |name, rels|
      get_node_paths(rels.first)
    end.flatten(1)

    node_paths += rels_by_ends.map do |ends, rels|
      get_node_paths(rels.first)
    end.flatten(1)
  end

  node_paths = node_paths.map do |path|
    path.map { |n| project(n.lat, n.lon, ZOOM) }
  end

  save("#{File.basename(data_path, '.json')}.svg", node_paths)
end

def get_node_paths(rel)
  rel.members
    .select { |e| e[:type] == 'way' && e[:role].empty? }
    .map { |e| $ways[e[:ref]] }
    .map { |w| w.nodes.map { |n| $nodes[n] } }
end

class Subway < Mustache
  self.template_path = __dir__
end

def save(filename, node_paths)
  points = node_paths.flatten(1)
  xx, yy = points.map { |p| p[0] }, points.map { |p| p[1] }
  min_x, min_y = xx.min, yy.min

  t = Subway.new
  t[:paths] =
    node_paths.map do |path|
      s = path.map { |p| "#{p[0]-min_x} #{p[1]-min_y}" }.join(' ')
      %(<path d="M#{s}"/>)
    end.join("\n    ")

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
