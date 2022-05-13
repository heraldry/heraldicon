(ns heraldicon.util.colour)

(def html-names
  {:indianred "#cd5c5c"
   :lightcoral "#f08080"
   :salmon "#fa8072"
   :darksalmon "#e9967a"
   :lightsalmon "#ffa07a"
   :crimson "#dc143c"
   :red "#ff0000"
   :firebrick "#b22222"
   :darkred "#8b0000"
   :pink "#ffc0cb"
   :lightpink "#ffb6c1"
   :hotpink "#ff69b4"
   :deeppink "#ff1493"
   :mediumvioletred "#c71585"
   :palevioletred "#db7093"
   :coral "#ff7f50"
   :tomato "#ff6347"
   :orangered "#ff4500"
   :darkorange "#ff8c00"
   :orange "#ffa500"
   :gold "#ffd700"
   :yellow "#ffff00"
   :lightyellow "#ffffe0"
   :lemonchiffon "#fffacd"
   :lightgoldenrodyellow "#fafad2"
   :papayawhip "#ffefd5"
   :moccasin "#ffe4b5"
   :peachpuff "#ffdab9"
   :palegoldenrod "#eee8aa"
   :khaki "#f0e68c"
   :darkkhaki "#bdb76b"
   :lavender "#e6e6fa"
   :thistle "#d8bfd8"
   :plum "#dda0dd"
   :violet "#ee82ee"
   :orchid "#da70d6"
   :fuchsia "#ff00ff"
   :magenta "#ff00ff"
   :mediumorchid "#ba55d3"
   :mediumpurple "#9370db"
   :rebeccapurple "#663399"
   :blueviolet "#8a2be2"
   :darkviolet "#9400d3"
   :darkorchid "#9932cc"
   :darkmagenta "#8b008b"
   :purple "#800080"
   :indigo "#4b0082"
   :slateblue "#6a5acd"
   :darkslateblue "#483d8b"
   :mediumslateblue "#7b68ee"
   :greenyellow "#adff2f"
   :chartreuse "#7fff00"
   :lawngreen "#7cfc00"
   :lime "#00ff00"
   :limegreen "#32cd32"
   :palegreen "#98fb98"
   :lightgreen "#90ee90"
   :mediumspringgreen "#00fa9a"
   :springgreen "#00ff7f"
   :mediumseagreen "#3cb371"
   :seagreen "#2e8b57"
   :forestgreen "#228b22"
   :green "#008000"
   :darkgreen "#006400"
   :yellowgreen "#9acd32"
   :olivedrab "#6b8e23"
   :olive "#808000"
   :darkolivegreen "#556b2f"
   :mediumaquamarine "#66cdaa"
   :darkseagreen "#8fbc8b"
   :lightseagreen "#20b2aa"
   :darkcyan "#008b8b"
   :teal "#008080"
   :aqua "#00ffff"
   :cyan "#00ffff"
   :lightcyan "#e0ffff"
   :paleturquoise "#afeeee"
   :aquamarine "#7fffd4"
   :turquoise "#40e0d0"
   :mediumturquoise "#48d1cc"
   :darkturquoise "#00ced1"
   :cadetblue "#5f9ea0"
   :steelblue "#4682b4"
   :lightsteelblue "#b0c4de"
   :powderblue "#b0e0e6"
   :lightblue "#add8e6"
   :skyblue "#87ceeb"
   :lightskyblue "#87cefa"
   :deepskyblue "#00bfff"
   :dodgerblue "#1e90ff"
   :cornflowerblue "#6495ed"
   :royalblue "#4169e1"
   :blue "#0000ff"
   :mediumblue "#0000cd"
   :darkblue "#00008b"
   :navy "#000080"
   :midnightblue "#191970"
   :cornsilk "#fff8dc"
   :blanchedalmond "#ffebcd"
   :bisque "#ffe4c4"
   :navajowhite "#ffdead"
   :wheat "#f5deb3"
   :burlywood "#deb887"
   :tan "#d2b48c"
   :rosybrown "#bc8f8f"
   :sandybrown "#f4a460"
   :goldenrod "#daa520"
   :darkgoldenrod "#b8860b"
   :peru "#cd853f"
   :chocolate "#d2691e"
   :saddlebrown "#8b4513"
   :sienna "#a0522d"
   :brown "#a52a2a"
   :maroon "#800000"
   :white "#ffffff"
   :snow "#fffafa"
   :honeydew "#f0fff0"
   :mintcream "#f5fffa"
   :azure "#f0ffff"
   :aliceblue "#f0f8ff"
   :ghostwhite "#f8f8ff"
   :whitesmoke "#f5f5f5"
   :seashell "#fff5ee"
   :beige "#f5f5dc"
   :oldlace "#fdf5e6"
   :floralwhite "#fffaf0"
   :ivory "#fffff0"
   :antiquewhite "#faebd7"
   :linen "#faf0e6"
   :lavenderblush "#fff0f5"
   :mistyrose "#ffe4e1"
   :gainsboro "#dcdcdc"
   :lightgray "#d3d3d3"
   :silver "#c0c0c0"
   :darkgray "#a9a9a9"
   :gray "#808080"
   :dimgray "#696969"
   :lightslategray "#778899"
   :slategray "#708090"
   :darkslategray "#2f4f4f"
   :black "#000000"})

(defn -expand-three-hex [colour]
  (if (and (-> colour count (= 4))
           (-> colour first (= "#")))
    (let [[_ r g b] colour]
      (str "#" r r g g b b))
    nil))

(defn -to-hex-2 [v]
  (let [s (.toString v 16)]
    (if (-> s count (= 1))
      (str "0" s)
      s)))

(defn hex-colour [r g b]
  (str "#"
       (-to-hex-2 r)
       (-to-hex-2 g)
       (-to-hex-2 b)))

(defn -convert-rgb [colour]
  (try
    (let [[_ r g b] (re-matches #"(?i)rgb\( *([0-9]*) *, *([0-9]*) *, *([0-9]*) *\)" colour)
          rv (js/parseInt r)
          gv (js/parseInt g)
          bv (js/parseInt b)]
      (if (and r g b)
        (hex-colour rv gv bv)
        nil))
    (catch :default _
      nil)))

(defn normalize [colour]
  (or (get html-names (keyword colour))
      (-expand-three-hex colour)
      (-convert-rgb colour)
      colour))

(defn darken [colour]
  (let [[_ r1 r2 g1 g2 b1 b2] (normalize colour)
        r (js/parseInt (str r1 r2) 16)
        g (js/parseInt (str g1 g2) 16)
        b (js/parseInt (str b1 b2) 16)
        new-r (quot r 2)
        new-g (quot g 2)
        new-b (quot b 2)]
    (hex-colour new-r new-g new-b)))

(defn desaturate [colour]
  ;; this is an approximation, but it should work for the purpose
  (let [[_ r1 r2 g1 g2 b1 b2] (normalize colour)
        r (js/parseInt (str r1 r2) 16)
        g (js/parseInt (str g1 g2) 16)
        b (js/parseInt (str b1 b2) 16)
        l (int (+ (* 0.3 r)
                  (* 0.6 g)
                  (* 0.1 b)))]
    (hex-colour l l l)))

(defn percent-grey [percentage]
  (let [v (-> percentage
              (/ 100)
              (* 255)
              int)]
    (hex-colour v v v)))
