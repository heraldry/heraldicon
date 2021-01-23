(ns heraldry.coat-of-arms.charge
  (:require ["svgpath" :as svgpath]
            [clojure.string :as s]
            [clojure.walk :as walk]
            [heraldry.coat-of-arms.division :as division]
            [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.coat-of-arms.field-environment :as field-environment]
            [heraldry.coat-of-arms.geometry :as geometry]
            [heraldry.coat-of-arms.line :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.tincture :as tincture]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(defn find-charge [charge-map [group & rest]]
  (let [next (get-in charge-map [:groups group])]
    (if rest
      (recur next rest)
      next)))

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

(defn remove-outlines [data]
  (walk/postwalk #(if (and (vector? %)
                           (->> % first (get #{:stroke :fill}))
                           (->> % second (get #{"#000000" "#000" "black"})))
                    [(first %) "none"]
                    %)
                 data))

(defn replace-colours [data function]
  (walk/postwalk #(if (and (vector? %)
                           (-> % second string?)
                           (->> % first (get #{:stroke :fill}))
                           (-> % second (not= "none")))
                    [(first %) (function (second %))]
                    %)
                 data))

(defn get-replacement [kind provided-placeholder-colours]
  (let [replacement (get provided-placeholder-colours kind)]
    (if (or (nil? replacement)
            (= replacement :none))
      nil
      replacement)))

(defn make-mask [data placeholder-colours provided-placeholder-colours outline-mode]
  (let [mask-id (util/id "mask")
        mask-inverted-id (util/id "mask")
        mask (replace-colours
              data
              (fn [colour]
                (let [colour-lower (s/lower-case colour)
                      kind (get placeholder-colours colour-lower)
                      replacement (get-replacement kind provided-placeholder-colours)]
                  (if (or (= kind :keep)
                          (and (not (#{:transparent :primary} outline-mode))
                               (= kind :outline))
                          replacement)
                    "#fff"
                    "#000"))))
        mask-inverted (replace-colours
                       data
                       (fn [colour]
                         (let [colour-lower (s/lower-case colour)
                               kind (get placeholder-colours colour-lower)
                               replacement (get-replacement kind provided-placeholder-colours)]
                           (if (or (= kind :keep)
                                   (and (not (#{:primary} outline-mode))
                                        (= kind :outline))
                                   replacement)
                             "#000"
                             "#fff"))))]
    [mask-id mask mask-inverted-id mask-inverted]))

(defn counterchange-field [field {:keys [division]}]
  (let [type (:type division)]
    (-> field
        (dissoc :content)
        (assoc :division {:type type
                          :line (:line division)
                          :fields (-> (division/default-fields type)
                                      (assoc-in [0 :content :tincture] (get-in division [:fields 1 :content :tincture]))
                                      (assoc-in [1 :content :tincture] (get-in division [:fields 0 :content :tincture])))}))))

(defn counterchangable? [field parent]
  (and (:counterchanged? field)
       (division/counterchangable? (-> parent :division))))

(def default-options
  {:position position/default-options
   :geometry geometry/default-options
   :escutcheon {:type :choice
                :choices escutcheon/choices
                :default :heater}})

(defn options [charge]
  (when charge
    (let [type (:type charge)]
      (->
       default-options
       (options/merge
        (->
         (get {:escutcheon {:geometry {:size {:default 30}
                                       :mirrored? nil}}
               :roundel {:geometry {:mirrored? nil
                                    :reversed? nil}}
               :annulet {:geometry {:mirrored? nil
                                    :reversed? nil}}
               :billet {:geometry {:mirrored? nil
                                   :reversed? nil}}
               :lozenge {:geometry {:mirrored? nil
                                    :reversed? nil}}
               :fusil {:geometry {:mirrored? nil
                                  :reversed? nil}}
               :mascle {:geometry {:mirrored? nil
                                   :reversed? nil}}
               :rustre {:geometry {:mirrored? nil
                                   :reversed? nil}}
               :crescent {:geometry {:mirrored? nil}}}
              type)
         (cond->
          (not= type :escutcheon) (assoc :escutcheon nil))))))))

(defn make-charge
  [{:keys [field hints] :as charge} parent environment {:keys [render-options] :as context} arg function]
  (let [{:keys [position geometry]} (options/sanitize charge (options charge))
        {:keys [size stretch rotation
                mirrored? reversed?]} geometry
        position-point (position/calculate position environment :fess)
        arg-value (get environment arg)
        target-arg-value (-> arg-value
                             (* size)
                             (/ 100))
        scale-x (if mirrored? -1 1)
        scale-y (* (if reversed? -1 1) stretch)
        {:keys [shape
                mask
                charge-width
                charge-height]} (function target-arg-value)
        charge-shape (-> shape
                         svg/make-path
                         (->
                          (svgpath)
                          (.scale scale-x scale-y)
                          (.toString))
                         (cond->
                          (:squiggly? render-options) line/squiggly-path
                          (not= rotation 0) (->
                                             (svgpath)
                                             (.rotate rotation)
                                             (.toString)))
                         (line/translate (:x position-point) (:y position-point)))
        mask-shape (when mask
                     (-> mask
                         svg/make-path
                         (->
                          (svgpath)
                          (.scale scale-x scale-y)
                          (.toString))
                         (cond->
                          (:squiggly? render-options) line/squiggly-path
                          (not= rotation 0) (->
                                             (svgpath)
                                             (.rotate rotation)
                                             (.toString)))
                         (line/translate (:x position-point) (:y position-point))))
        [min-x max-x min-y max-y] (svg/rotated-bounding-box (v//
                                                             (v/v charge-width
                                                                  charge-height)
                                                             -2)
                                                            (v//
                                                             (v/v charge-width
                                                                  charge-height)
                                                             -2)
                                                            rotation
                                                            :scale (v/v scale-x scale-y))
        box-size (v/v (- max-x min-x)
                      (- max-y min-y))
        parts [[charge-shape
                [(v/- position-point
                      (v// box-size 2))
                 (v/+ position-point
                      (v// box-size 2))]
                mask-shape]]
        field (if (counterchangable? field parent)
                (counterchange-field field parent)
                field)]
    [division/make-division
     :charge-pale [field] parts
     [:all]
     (when (or (:outline? render-options)
               (-> hints :outline-mode (= :keep)))
       [:g division/outline-style
        [:path {:d charge-shape}]
        (when mask-shape
          [:path {:d mask-shape}])])
     environment charge context]))

(defn escutcheon
  {:display-name "Escutcheon"}
  [charge parent environment context]
  (let [{:keys [escutcheon]} (options/sanitize charge (options charge))]
    (make-charge charge parent environment context
                 :width
                 (fn [width]
                   (let [env (field-environment/transform-to-width
                              (escutcheon/field escutcheon) width)
                         env-fess (-> env :points :fess)]
                     {:shape (line/translate (:shape env)
                                             (-> env-fess :x -)
                                             (-> env-fess :y -))
                      :charge-width width
                      :charge-height width})))))

(defn roundel
  {:display-name "Roundel"}
  [charge parent environment context]
  (make-charge charge parent environment context
               :width
               (fn [width]
                 (let [radius (/ width 2)]
                   {:shape ["m" (v/v radius 0)
                            ["a" radius radius
                             0 0 0 (v/v (- width) 0)]
                            ["a" radius radius
                             0 0 0 width 0]
                            "z"]
                    :charge-width width
                    :charge-height width}))))

(defn annulet
  {:display-name "Annulet"}
  [charge parent environment context]
  (make-charge charge parent environment context
               :width
               (fn [width]
                 (let [radius (/ width 2)
                       hole-radius (* radius 0.6)]
                   {:shape ["m" (v/v radius 0)
                            ["a" radius radius
                             0 0 0 (v/v (- width) 0)]
                            ["a" radius radius
                             0 0 0 width 0]
                            "z"]
                    :mask ["m" (v/v hole-radius 0)
                           ["a" hole-radius hole-radius
                            0 0 0 (v/v (* hole-radius -2) 0)]
                           ["a" hole-radius hole-radius
                            0 0 0 (* hole-radius 2) 0]
                           "z"]
                    :charge-width width
                    :charge-height width}))))

(defn billet
  {:display-name "Billet"}
  [charge parent environment context]
  (make-charge charge parent environment context
               :height
               (fn [height]
                 (let [width (/ height 2)
                       width-half (/ width 2)
                       height-half (/ height 2)]
                   {:shape ["m" (v/v (- width-half) (- height-half))
                            "h" width
                            "v" height
                            "h" (- width)
                            "z"]
                    :charge-width width
                    :charge-height height}))))

(defn lozenge
  {:display-name "Lozenge"}
  [charge parent environment context]
  (make-charge charge parent environment context
               :height
               (fn [height]
                 (let [width (/ height 1.3)
                       width-half (/ width 2)
                       height-half (/ height 2)]
                   {:shape ["m" (v/v 0 (- height-half))
                            "l" (v/v width-half height-half)
                            "l " (v/v (- width-half) height-half)
                            "l" (v/v (- width-half) (- height-half))
                            "z"]
                    :charge-width width
                    :charge-height height}))))

(defn fusil
  {:display-name "Fusil"}
  [charge parent environment context]
  (make-charge charge parent environment context
               :height
               (fn [height]
                 (let [width (/ height 2)
                       width-half (/ width 2)
                       height-half (/ height 2)]
                   {:shape ["m" (v/v 0 (- height-half))
                            "l" (v/v width-half height-half)
                            "l " (v/v (- width-half) height-half)
                            "l" (v/v (- width-half) (- height-half))
                            "z"]
                    :charge-width width
                    :charge-height height}))))

(defn mascle
  {:display-name "Mascle"}
  [charge parent environment context]
  (make-charge charge parent environment context
               :height
               (fn [height]
                 (let [width (/ height 1.3)
                       width-half (/ width 2)
                       height-half (/ height 2)
                       hole-width (* width 0.55)
                       hole-height (* height 0.55)
                       hole-width-half (/ hole-width 2)
                       hole-height-half (/ hole-height 2)]
                   {:shape ["m" (v/v 0 (- height-half))
                            "l" (v/v width-half height-half)
                            "l " (v/v (- width-half) height-half)
                            "l" (v/v (- width-half) (- height-half))
                            "z"]
                    :mask ["m" (v/v 0 (- hole-height-half))
                           "l" (v/v hole-width-half hole-height-half)
                           "l " (v/v (- hole-width-half) hole-height-half)
                           "l" (v/v (- hole-width-half) (- hole-height-half))
                           "z"]
                    :charge-width width
                    :charge-height height}))))

(defn rustre
  {:display-name "Rustre"}
  [charge parent environment context]
  (make-charge charge parent environment context
               :height
               (fn [height]
                 (let [width (/ height 1.3)
                       width-half (/ width 2)
                       height-half (/ height 2)
                       hole-radius (/ width 4)]
                   {:shape ["m" (v/v 0 (- height-half))
                            "l" (v/v width-half height-half)
                            "l " (v/v (- width-half) height-half)
                            "l" (v/v (- width-half) (- height-half))
                            "z"]
                    :mask ["m" (v/v hole-radius 0)
                           ["a" hole-radius hole-radius
                            0 0 0 (v/v (* hole-radius -2) 0)]
                           ["a" hole-radius hole-radius
                            0 0 0 (* hole-radius 2) 0]
                           "z"]
                    :charge-width width
                    :charge-height height}))))

(defn crescent
  {:display-name "Crescent"}
  [charge parent environment context]
  (make-charge charge parent environment context
               :width
               (fn [width]
                 (let [radius (/ width 2)
                       inner-radius (* radius
                                       0.75)
                       horn-angle -45
                       horn-point-x (* radius
                                       (-> horn-angle
                                           (* Math/PI)
                                           (/ 180)
                                           Math/cos))
                       horn-point-y (* radius
                                       (-> horn-angle
                                           (* Math/PI)
                                           (/ 180)
                                           Math/sin))
                       horn-point-1 (v/v horn-point-x horn-point-y)
                       horn-point-2 (v/v (- horn-point-x) horn-point-y)]
                   {:shape ["m" horn-point-1
                            ["a" radius radius
                             0 1 1 (v/- horn-point-2 horn-point-1)]
                            ["a" inner-radius inner-radius
                             0 1 0 (v/- horn-point-1 horn-point-2)]
                            "z"]
                    :charge-width width
                    :charge-height width}))))

(def charges
  [#'roundel
   #'annulet
   #'billet
   #'escutcheon
   #'lozenge
   #'fusil
   #'mascle
   #'rustre
   #'crescent])

(def kinds-function-map
  (->> charges
       (map (fn [function]
              [(-> function meta :name keyword) function]))
       (into {})))

(def choices
  (->> charges
       (map (fn [function]
              [(-> function meta :display-name) (-> function meta :name keyword)]))))

(defn render-other-charge [{:keys [type field tincture hints variant] :as charge} parent
                           environment {:keys [render-field render-options load-charge-data] :as context}]
  (if-let [charge-data (if (keyword? type)
                         (:data (load-charge-data variant))
                         type)]
    (let [{:keys [position geometry]} (options/sanitize charge (options charge))
          {:keys [size stretch
                  mirrored? reversed?
                  rotation]} geometry
          ;; since size now is filled with a default, check whether it was set at all,
          ;; if not, then use nil
          ;; TODO: this probably needs a better mechanism and form representation
          size (if (-> charge :geometry :size) size nil)
          points (:points environment)
          top (:top points)
          bottom (:bottom points)
          left (:left points)
          right (:right points)
          positional-charge-width (js/parseFloat (:width charge-data))
          positional-charge-height (js/parseFloat (:height charge-data))
          width (:width environment)
          height (:height environment)
          center-point (position/calculate position environment :fess)
          min-x-distance (min (- (:x center-point) (:x left))
                              (- (:x right) (:x center-point)))
          min-y-distance (min (- (:y center-point) (:y top))
                              (- (:y bottom) (:y center-point)))
          target-width (if size
                         (-> size
                             (* width)
                             (/ 100))
                         (* (* min-x-distance 2) 0.8))
          target-height (/ (if size
                             (-> size
                                 (* height)
                                 (/ 100))
                             (* (* min-y-distance 2) 0.7))
                           stretch)
          scale-x (* (if mirrored? -1 1)
                     (min (/ target-width positional-charge-width)
                          (/ target-height positional-charge-height)))
          scale-y (* (if reversed? -1 1)
                     (* (Math/abs scale-x) stretch))
          adjusted-charge (-> charge-data
                              :data
                              fix-string-style-values
                              (cond->
                               (not (or (-> hints :outline-mode (not= :remove))
                                        (:outline? render-options))) remove-outlines
                               (and (:squiggly? render-options)
                                    (get #{:roundel
                                           :fusil
                                           :billet} type)) line/squiggly-paths))
          placeholder-colours (-> charge-data
                                  :colours
                                  (cond->>
                                   (:preview-original? render-options)
                                    (into {}
                                          (map (fn [[k _]]
                                                 [k :keep])))))
          [mask-id mask
           mask-inverted-id mask-inverted] (make-mask adjusted-charge
                                                      placeholder-colours
                                                      tincture
                                                      (:outline-mode hints))
          coloured-charge (replace-colours
                           adjusted-charge
                           (fn [colour]
                             (let [colour-lower (s/lower-case colour)
                                   kind (get placeholder-colours colour-lower)
                                   replacement (get-replacement kind tincture)]
                               (if replacement
                                 (tincture/pick replacement render-options)
                                 colour))))
          clip-path-id (util/id "clip-path")
          shift (-> (v/v positional-charge-width positional-charge-height)
                    (v// 2)
                    (v/-))
          [min-x max-x min-y max-y] (svg/rotated-bounding-box
                                     shift
                                     (v/dot shift (v/v -1 -1))
                                     rotation
                                     :scale (v/v scale-x scale-y))
          clip-size (v/v (- max-x min-x) (- max-y min-y))
          position (-> clip-size
                       (v/-)
                       (v// 2)
                       (v/+ center-point))
          charge-environment (field-environment/create
                              (svg/make-path ["M" position
                                              "l" (v/v (:x clip-size) 0)
                                              "l" (v/v 0 (:y clip-size))
                                              "l" (v/v (- (:x clip-size)) 0)
                                              "l" (v/v 0 (- (:y clip-size)))
                                              "z"])
                              {:parent field
                               :context [:charge]
                               :bounding-box (svg/bounding-box
                                              [position (v/+ position
                                                             clip-size)])
                               :override-environment (when (or (:inherit-environment? field)
                                                               (counterchangable? field parent)) environment)})
          field (if (counterchangable? field parent)
                  (counterchange-field field parent)
                  field)]
      [:<>
       [:defs
        [:mask {:id mask-id}
         mask]
        [:mask {:id mask-inverted-id}
         mask-inverted]
        [:clipPath {:id clip-path-id}
         [:rect {:x 0
                 :y 0
                 :width positional-charge-width
                 :height positional-charge-height
                 :fill "#fff"}]]]
       (let [transform (str "translate(" (:x center-point) "," (:y center-point) ")"
                            "rotate(" rotation ")"
                            "scale(" scale-x "," scale-y ")"
                            "translate(" (-> shift :x) "," (-> shift :y) ")")
             reverse-transform (str "translate(" (-> shift :x -) "," (-> shift :y -) ")"
                                    "scale(" (/ 1 scale-x) "," (/ 1 scale-y) ")"
                                    "rotate(" (- rotation) ")"
                                    "translate(" (- (:x center-point)) "," (- (:y center-point)) ")")]
         [:g {:transform transform
              :clip-path (str "url(#" clip-path-id ")")}
          [:g {:mask (str "url(#" mask-inverted-id ")")}
           [:g {:transform reverse-transform}
            [render-field field charge-environment (-> context
                                                       (update :db-path conj :field))]]]
          [:g {:mask (str "url(#" mask-id ")")}
           coloured-charge]])])
    [:<>]))

(defn render [{:keys [type] :as charge} parent environment context]
  (let [function (get kinds-function-map type)]
    (if function
      [function charge parent environment context]
      [render-other-charge charge parent environment context])))
