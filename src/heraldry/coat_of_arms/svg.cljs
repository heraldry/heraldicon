(ns heraldry.coat-of-arms.svg
  (:require ["svg-path-properties" :as svg-path-properties]
            ["svgpath" :as svgpath]
            [clojure.string :as s]
            [clojure.walk :as walk]
            [heraldry.coat-of-arms.catmullrom :as catmullrom]
            [heraldry.coat-of-arms.random :as random]
            [heraldry.coat-of-arms.vector :as v]))

(defn clean-path [d]
  (s/replace d #"l *0 *[, ] *0" ""))

(defn new-path-browser [d]
  (let [p (js/document.createElementNS "http://www.w3.org/2000/svg" "path")]
    (.setAttribute p "d" d)
    p))

(defn new-path [d]
  (->> d
       clean-path
       (new svg-path-properties/svgPathProperties)))

(defn points [^js/Object path n]
  (let [length (.getTotalLength path)
        n      (if (= n :length)
                 (-> length
                     Math/floor
                     inc)
                 n)]
    (mapv (fn [i]
            (let [x (-> length (* i) (/ (dec n)))
                  p (.getPointAtLength path x)]
              (v/v (.-x p) (.-y p)))) (range n))))

(defn min-max-x-y [[{x :x y :y} & rest]]
  (reduce (fn [[min-x max-x min-y max-y] {x :x y :y}]
            [(min min-x x)
             (max max-x x)
             (min min-y y)
             (max max-y y)])
          [x x y y]
          rest))

(defn avg-x-y [[p & rest]]
  (let [[s n] (reduce (fn [[s n] p]
                        [(v/+ s p)
                         (inc n)])
                      [p 1]
                      rest)]
    (v// s n)))

(defn bounding-box-from-path [d]
  (let [path   (new-path d)
        points (points path 50)
        box    (min-max-x-y points)]
    box))

(defn bounding-box [points]
  (min-max-x-y points))

(defn center [d]
  (let [path   (new-path d)
        points (points path 50)
        center (avg-x-y points)]
    center))

(defn make-path [v]
  (cond
    (string? v)     v
    (and (map? v)
         (:x v)
         (:y v))    (str (:x v) "," (:y v))
    (sequential? v) (s/join " " (map make-path v))
    :else           (str v)))

(defn rotated-bounding-box [{x1 :x y1 :y :as p1} {x2 :x y2 :y :as p2} rotation & {:keys [middle scale]}]
  (let [middle (or middle
                   (v/avg p1 p2))
        scale  (or scale
                   (v/v 1 1))
        points [(v/+ middle
                     (v/rotate (v/dot (v/- (v/v x1 y1)
                                           middle)
                                      scale) rotation))
                (v/+ middle
                     (v/rotate (v/dot (v/- (v/v x2 y1)
                                           middle)
                                      scale) rotation))
                (v/+ middle
                     (v/rotate (v/dot (v/- (v/v x1 y2)
                                           middle)
                                      scale) rotation))
                (v/+ middle
                     (v/rotate (v/dot (v/- (v/v x2 y2)
                                           middle)
                                      scale) rotation))]]
    (bounding-box points)))

(def html-colours
  {:indianred            "#cd5c5c"
   :lightcoral           "#f08080"
   :salmon               "#fa8072"
   :darksalmon           "#e9967a"
   :lightsalmon          "#ffa07a"
   :crimson              "#dc143c"
   :red                  "#ff0000"
   :firebrick            "#b22222"
   :darkred              "#8b0000"
   :pink                 "#ffc0cb"
   :lightpink            "#ffb6c1"
   :hotpink              "#ff69b4"
   :deeppink             "#ff1493"
   :mediumvioletred      "#c71585"
   :palevioletred        "#db7093"
   :coral                "#ff7f50"
   :tomato               "#ff6347"
   :orangered            "#ff4500"
   :darkorange           "#ff8c00"
   :orange               "#ffa500"
   :gold                 "#ffd700"
   :yellow               "#ffff00"
   :lightyellow          "#ffffe0"
   :lemonchiffon         "#fffacd"
   :lightgoldenrodyellow "#fafad2"
   :papayawhip           "#ffefd5"
   :moccasin             "#ffe4b5"
   :peachpuff            "#ffdab9"
   :palegoldenrod        "#eee8aa"
   :khaki                "#f0e68c"
   :darkkhaki            "#bdb76b"
   :lavender             "#e6e6fa"
   :thistle              "#d8bfd8"
   :plum                 "#dda0dd"
   :violet               "#ee82ee"
   :orchid               "#da70d6"
   :fuchsia              "#ff00ff"
   :magenta              "#ff00ff"
   :mediumorchid         "#ba55d3"
   :mediumpurple         "#9370db"
   :rebeccapurple        "#663399"
   :blueviolet           "#8a2be2"
   :darkviolet           "#9400d3"
   :darkorchid           "#9932cc"
   :darkmagenta          "#8b008b"
   :purple               "#800080"
   :indigo               "#4b0082"
   :slateblue            "#6a5acd"
   :darkslateblue        "#483d8b"
   :mediumslateblue      "#7b68ee"
   :greenyellow          "#adff2f"
   :chartreuse           "#7fff00"
   :lawngreen            "#7cfc00"
   :lime                 "#00ff00"
   :limegreen            "#32cd32"
   :palegreen            "#98fb98"
   :lightgreen           "#90ee90"
   :mediumspringgreen    "#00fa9a"
   :springgreen          "#00ff7f"
   :mediumseagreen       "#3cb371"
   :seagreen             "#2e8b57"
   :forestgreen          "#228b22"
   :green                "#008000"
   :darkgreen            "#006400"
   :yellowgreen          "#9acd32"
   :olivedrab            "#6b8e23"
   :olive                "#808000"
   :darkolivegreen       "#556b2f"
   :mediumaquamarine     "#66cdaa"
   :darkseagreen         "#8fbc8b"
   :lightseagreen        "#20b2aa"
   :darkcyan             "#008b8b"
   :teal                 "#008080"
   :aqua                 "#00ffff"
   :cyan                 "#00ffff"
   :lightcyan            "#e0ffff"
   :paleturquoise        "#afeeee"
   :aquamarine           "#7fffd4"
   :turquoise            "#40e0d0"
   :mediumturquoise      "#48d1cc"
   :darkturquoise        "#00ced1"
   :cadetblue            "#5f9ea0"
   :steelblue            "#4682b4"
   :lightsteelblue       "#b0c4de"
   :powderblue           "#b0e0e6"
   :lightblue            "#add8e6"
   :skyblue              "#87ceeb"
   :lightskyblue         "#87cefa"
   :deepskyblue          "#00bfff"
   :dodgerblue           "#1e90ff"
   :cornflowerblue       "#6495ed"
   :royalblue            "#4169e1"
   :blue                 "#0000ff"
   :mediumblue           "#0000cd"
   :darkblue             "#00008b"
   :navy                 "#000080"
   :midnightblue         "#191970"
   :cornsilk             "#fff8dc"
   :blanchedalmond       "#ffebcd"
   :bisque               "#ffe4c4"
   :navajowhite          "#ffdead"
   :wheat                "#f5deb3"
   :burlywood            "#deb887"
   :tan                  "#d2b48c"
   :rosybrown            "#bc8f8f"
   :sandybrown           "#f4a460"
   :goldenrod            "#daa520"
   :darkgoldenrod        "#b8860b"
   :peru                 "#cd853f"
   :chocolate            "#d2691e"
   :saddlebrown          "#8b4513"
   :sienna               "#a0522d"
   :brown                "#a52a2a"
   :maroon               "#800000"
   :white                "#ffffff"
   :snow                 "#fffafa"
   :honeydew             "#f0fff0"
   :mintcream            "#f5fffa"
   :azure                "#f0ffff"
   :aliceblue            "#f0f8ff"
   :ghostwhite           "#f8f8ff"
   :whitesmoke           "#f5f5f5"
   :seashell             "#fff5ee"
   :beige                "#f5f5dc"
   :oldlace              "#fdf5e6"
   :floralwhite          "#fffaf0"
   :ivory                "#fffff0"
   :antiquewhite         "#faebd7"
   :linen                "#faf0e6"
   :lavenderblush        "#fff0f5"
   :mistyrose            "#ffe4e1"
   :gainsboro            "#dcdcdc"
   :lightgray            "#d3d3d3"
   :silver               "#c0c0c0"
   :darkgray             "#a9a9a9"
   :gray                 "#808080"
   :dimgray              "#696969"
   :lightslategray       "#778899"
   :slategray            "#708090"
   :darkslategray        "#2f4f4f"
   :black                "#000000"})

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

(defn -convert-rgb [colour]
  (try
    (let [[_ r g b] (re-matches #"(?i)rgb\( *([0-9]*) *, *([0-9]*) *, *([0-9]*) *\)" colour)
          rv        (js/parseInt r)
          gv        (js/parseInt g)
          bv        (js/parseInt b)]
      (if (and r g b)
        (str "#"
             (-to-hex-2 rv)
             (-to-hex-2 gv)
             (-to-hex-2 bv))
        nil))
    (catch :default _
      nil)))

(defn normalize-colour [colour]
  (or (get html-colours (keyword colour))
      (-expand-three-hex colour)
      (-convert-rgb colour)
      colour))

(defn jiggle [[previous
               {:keys [x y] :as current}
               _]]
  (let [dist          (-> current
                          (v/- previous)
                          (v/abs))
        jiggle-radius (/ dist 4)
        dx            (- (* (random/float) jiggle-radius)
                         jiggle-radius)
        dy            (- (* (random/float) jiggle-radius)
                         jiggle-radius)]
    {:x (+ x dx)
     :y (+ y dy)}))

(defn squiggly-path [path & {:keys [seed]}]
  (random/seed (if seed
                 [seed path]
                 path))
  (let [points   (-> path
                     new-path
                     (points :length))
        points   (vec (concat [(first points)]
                              (map jiggle (partition 3 1 points))
                              [(last points)]))
        curve    (catmullrom/catmullrom points)
        new-path (catmullrom/curve->svg-path-relative curve)]
    new-path))

(defn squiggly-paths [data]
  (walk/postwalk #(cond-> %
                    (vector? %) ((fn [v]
                                   (if (= (first v) :d)
                                     [:d (squiggly-path (second v))]
                                     v))))
                 data))

(defn translate [path dx dy]
  (-> path
      svgpath
      (.translate dx dy)
      .toString))

(defn stitch [path]
  ;; TODO: this can be improved, it already broke some things and caused unexpected behaviour,
  ;; because the 'e' was not part of the pattern
  (s/replace path #"^M[ ]*[0-9.e-]+[, -] *[0-9.e-]+" ""))

(defn split-style-value [value]
  (-> value
      (s/split #";")
      (->>
       (map (fn [chunk]
              (-> chunk
                  (s/split #":" 2)
                  (as-> [key value]
                      [(keyword (s/trim key)) (s/trim value)])))))
      (into {})))

(defn fix-string-style-values [data]
  (walk/postwalk #(if (and (vector? %)
                           (-> % count (= 2))
                           (-> % first (= :style))
                           (-> % second string?))
                    [:style (split-style-value (second %))]
                    %)
                 data))
