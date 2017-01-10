require 'fileutils'
require 'pathname'

def screenshot(seed = nil)
    name = Pathname.new(SKETCH_ROOT).basename
    dir = Pathname.new('~/code/sketchbook/screenshots').expand_path.join(name)
    FileUtils.mkdir_p dir

    ts = Time.now.strftime '%Y-%m-%d_%H%M%S'
    seed = ('_s%05i' % seed) unless seed.nil?
    path = dir.join "#{ts}#{seed}_f#####.png"

    save_frame path.to_s
end