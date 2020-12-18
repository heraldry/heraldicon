(ns or.coad.field
  (:require ["svgpath" :as svgpath]
            [or.coad.svg :as svg]
            [or.coad.division :as division]))

(defn middle [[x1 y1] [x2 y2]]
  [(/ (+ x1 x2) 2)
   (/ (+ y1 y2) 2)])

(defn make-field [shape]
  (let [[min-x max-x min-y max-y] (svg/bounding-box shape)
        top-left [min-x min-y]
        top-right [max-x min-y]
        bottom-left [min-x max-y]
        bottom-right [max-x max-y]
        width (- max-x min-x)
        height (- max-y min-y)
        chief (middle top-left top-right)
        base (middle bottom-left bottom-right)
        ;; not actually center, but chosen such that bend lines at 45° run together in it
        fess [(first chief) (+ min-y (/ width 2))]
        dexter [min-x (second fess)]
        sinister [max-x (second fess)]
        honour (middle chief fess)
        nombril (middle honour base)]
    (-> {}
        (assoc-in [:shape] shape)
        (assoc-in [:box] [min-x min-y width height])
        (assoc-in [:points :chief] chief)
        (assoc-in [:points :base] base)
        (assoc-in [:points :fess] fess)
        (assoc-in [:points :dexter] dexter)
        (assoc-in [:points :sinister] sinister)
        (assoc-in [:points :honour] honour)
        (assoc-in [:points :nombril] nombril))))

(defn translate [[x y] [dx dy]]
  [(+ x dx)
   (+ y dy)])

(defn scale [[x y] f]
  [(* x f)
   (* y f)])

(defn transform-to-width [field target-width]
  (let [[min-x min-y width height] (:box field)
        [dx dy] [(- min-x) (- min-y)]
        scale-factor (/ target-width width)]
    (-> field
        #_(assoc-in [:transform] (str "scale(" scale-factor "," scale-factor ") "
                                      "translate(" dx "," dy ")"))
        (assoc-in [:shape] (-> (:shape field)
                               (svgpath)
                               (.translate dx dy)
                               (.scale scale-factor)
                               (.toString)))
        (assoc-in [:box] (vec (concat [0 0] (scale [width height] scale-factor))))
        (update-in [:points] merge (into {}
                                         (map (fn [[key value]]
                                                [key (-> value
                                                         (translate [dx dy])
                                                         (scale scale-factor))]) (:points field)))))))

(defn render [content field]
  (let [division (:division content)
        ordinaries (:ordinaries content)]
    [:<>
     [division/render division field]
     #_(for [[idx ordinary] (map-indexed vector ordinaries)]
         ^{:key idx} [ordinary/render ordinary])]))
