
# rotate a rect around the center and scale so it fits
translate width/2, height/2
angle = map1d(mouse_x, 0..width, -90..90)
h = Math.hypot(width/2, height/2)
min_y = h*sin((angle+45).radians)
max_y = h*sin((angle+225).radians)

rotate(-angle.radians)
scale(width/(max_y-min_y))



def get_bounding_dimensions(poly)
    vmin, vmax = *poly.vertices.reduce([Vec2D.new(width, height), Vec2D.new(0, 0)]) do |minmax, v|
        puts minmax
        minmax[0].x = v.x if v.x < minmax[0].x
        minmax[0].y = v.y if v.y < minmax[0].y
        minmax[1].x = v.x if v.x > minmax[1].x
        minmax[1].y = v.y if v.y > minmax[1].y
        puts minmax
        puts
        minmax
    end

    [vmax.x - vmin.x, vmax.y - vmin.y]
    #[vmin, vmax]
end

