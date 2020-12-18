(ns or.coad.field
  (:require ["svgpath" :as svgpath]
            [or.coad.svg :as svg]))

(defn middle [[x1 y1] [x2 y2]]
  [(/ (+ x1 x2) 2)
   (/ (+ y1 y2) 2)])

(defn make-field [shape meta]
  (let [[min-x max-x min-y max-y] (svg/bounding-box shape)
        top-left                  [min-x min-y]
        top-right                 [max-x min-y]
        bottom-left               [min-x max-y]
        bottom-right              [max-x max-y]
        width                     (- max-x min-x)
        height                    (- max-y min-y)
        chief                     (middle top-left top-right)
        base                      (middle bottom-left bottom-right)
        ;; not actually center, but chosen such that bend lines at 45° run together in it
        fess                      [(first chief) (+ min-y (/ width 2))]
        dexter                    [min-x (second fess)]
        sinister                  [max-x (second fess)]
        honour                    (middle chief fess)
        nombril                   (middle honour base)]
    (-> {}
        (assoc-in [:shape] shape)
        (assoc-in [:width] width)
        (assoc-in [:height] height)
        (assoc-in [:points :top-left] top-left)
        (assoc-in [:points :top-right] top-right)
        (assoc-in [:points :bottom-left] bottom-left)
        (assoc-in [:points :bottom-right] bottom-right)
        (assoc-in [:points :chief] chief)
        (assoc-in [:points :base] base)
        (assoc-in [:points :fess] fess)
        (assoc-in [:points :dexter] dexter)
        (assoc-in [:points :sinister] sinister)
        (assoc-in [:points :honour] honour)
        (assoc-in [:points :nombril] nombril)
        (assoc-in [:meta] meta))))

(defn transform-to-width [field target-width]
  (let [width         (:width field)
        [min-x min-y] (get-in field [:points :top-left])
        [dx dy]       [(- min-x) (- min-y)]
        scale-factor  (/ target-width width)]
    (-> field
        #_(assoc-in [:transform] (str "scale(" scale-factor "," scale-factor ") "
                                      "translate(" dx "," dy ")"))
        (assoc-in [:shape] (-> (:shape field)
                               (svgpath)
                               (.translate dx dy)
                               (.scale scale-factor)
                               (.toString)))
        (update-in [:width] * scale-factor)
        (update-in [:height] * scale-factor)
        (update-in [:points] merge (into {}
                                         (map (fn [[key value]]
                                                [key (-> value
                                                         (svg/translate [dx dy])
                                                         (svg/scale scale-factor))]) (:points field)))))))
