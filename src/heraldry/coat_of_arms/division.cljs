(ns heraldry.coat-of-arms.division
  (:require [clojure.string :as s]
            [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.field-environment :as field-environment]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(def overlap-stroke-width 0.1)
(def outline-stroke-width 0.5)

(def outline-style
  {:stroke          "#000"
   :stroke-width    outline-stroke-width
   :fill            "none"
   :stroke-linecap  "round"
   :stroke-linejoin "round"})

(defn diagonal-mode-choices [type]
  (let [options {:forty-five-degrees "45°"
                 :top-left-fess      "Top-left to origin"
                 :top-right-fess     "Top-right to origin"
                 :bottom-left-fess   "Bottom-left to origin"
                 :bottom-right-fess  "Bottom-right to origin"}]
    (->> type
         (get {:per-bend                    [:forty-five-degrees
                                             :top-left-fess]
               :bendy                       [:forty-five-degrees
                                             :top-left-fess]
               :per-bend-sinister           [:forty-five-degrees
                                             :top-right-fess]
               :bendy-sinister              [:forty-five-degrees
                                             :top-right-fess]
               :per-chevron                 [:forty-five-degrees
                                             :bottom-left-fess
                                             :bottom-right-fess]
               :per-saltire                 [:forty-five-degrees
                                             :top-left-fess
                                             :top-right-fess
                                             :bottom-left-fess
                                             :bottom-right-fess]
               :gyronny                     [:forty-five-degrees
                                             :top-left-fess
                                             :top-right-fess
                                             :bottom-left-fess
                                             :bottom-right-fess]
               :tierced-per-pairle          [:forty-five-degrees
                                             :top-left-fess
                                             :top-right-fess]
               :tierced-per-pairle-reversed [:forty-five-degrees
                                             :bottom-left-fess
                                             :bottom-right-fess]})
         (map (fn [key]
                [(get options key) key])))))

(def default-options
  {:line   line/default-options
   :layout {:origin          position/default-options
            :diagonal-mode   {:type    :choice
                              :default :top-left-fess}
            :num-fields-x    {:type     :range
                              :min      4
                              :max      20
                              :default  6
                              :integer? true}
            :num-fields-y    {:type     :range
                              :min      4
                              :max      20
                              :default  6
                              :integer? true}
            :num-base-fields {:type     :range
                              :min      2
                              :max      6
                              :default  2
                              :integer? true}
            :offset-x        {:type    :range
                              :min     -1
                              :max     1
                              :default 0}
            :offset-y        {:type    :range
                              :min     -1
                              :max     1
                              :default 0}
            :stretch-x       {:type    :range
                              :min     0.8
                              :max     1.2
                              :default 1}
            :stretch-y       {:type    :range
                              :min     0.8
                              :max     1.2
                              :default 1}}})

(defn pick-options [paths & values]
  (let [values  (first values)
        options (loop [options       {}
                       [path & rest] paths]
                  (let [next-options (-> options
                                         (assoc-in path (get-in default-options path)))]
                    (if (nil? rest)
                      next-options
                      (recur next-options rest))))]
    (loop [options              options
           [[key value] & rest] values]
      (let [next-options (if key
                           (assoc-in options key value)
                           options)]
        (if (nil? rest)
          next-options
          (recur next-options rest))))))

(defn options [division]
  (when division
    (->
     (case (:type division)
       :per-pale                    (pick-options [[:line]
                                                   [:layout :origin :point]
                                                   [:layout :origin :offset-x]]
                                                  {[:layout :origin :point :choices] position/point-choices-x})
       :per-fess                    (pick-options [[:line]
                                                   [:layout :origin :point]
                                                   [:layout :origin :offset-y]]
                                                  {[:layout :origin :point :choices] position/point-choices-y})
       :per-bend                    (pick-options [[:line]
                                                   [:layout :origin :point]
                                                   [:layout :origin :offset-y]
                                                   [:layout :diagonal-mode]]
                                                  {[:layout :diagonal-mode :choices] (diagonal-mode-choices :per-bend)
                                                   [:layout :origin :point :choices] position/point-choices-y})
       :per-bend-sinister           (pick-options [[:line]
                                                   [:layout :origin :point]
                                                   [:layout :origin :offset-y]
                                                   [:layout :diagonal-mode]]
                                                  {[:layout :diagonal-mode :choices] (diagonal-mode-choices :per-bend-sinister)
                                                   [:layout :diagonal-mode :default] :top-right-fess
                                                   [:layout :origin :point :choices] position/point-choices-y})
       :per-chevron                 (pick-options [[:line]
                                                   [:layout :origin :point]
                                                   [:layout :origin :offset-x]
                                                   [:layout :origin :offset-y]
                                                   [:layout :diagonal-mode]]
                                                  {[:layout :diagonal-mode :choices] (diagonal-mode-choices :per-chevron)
                                                   [:layout :diagonal-mode :default] :forty-five-degrees
                                                   [:layout :origin :point :choices] position/point-choices-y
                                                   [:line :offset :min]              0})
       :per-saltire                 (pick-options [[:line]
                                                   [:layout :origin :point]
                                                   [:layout :origin :offset-x]
                                                   [:layout :origin :offset-y]
                                                   [:layout :diagonal-mode]]
                                                  {[:layout :diagonal-mode :choices] (diagonal-mode-choices :per-saltire)
                                                   [:line :offset :min]              0})
       :quarterly                   (pick-options [[:line]
                                                   [:layout :origin :point]
                                                   [:layout :origin :offset-x]
                                                   [:layout :origin :offset-y]]
                                                  {[:line :offset :min] 0})
       :gyronny                     (pick-options [[:line]
                                                   [:layout :origin :point]
                                                   [:layout :origin :offset-x]
                                                   [:layout :origin :offset-y]
                                                   [:layout :diagonal-mode]]
                                                  {[:layout :diagonal-mode :choices] (diagonal-mode-choices :gyronny)
                                                   [:line :offset :min]              0})
       :paly                        (pick-options [[:line]
                                                   [:layout :num-base-fields]
                                                   [:layout :num-fields-x]
                                                   [:layout :offset-x]
                                                   [:layout :stretch-x]])
       :barry                       (pick-options [[:line]
                                                   [:layout :num-base-fields]
                                                   [:layout :num-fields-y]
                                                   [:layout :offset-y]
                                                   [:layout :stretch-y]])
       :bendy                       (pick-options [[:line]
                                                   [:layout :num-base-fields]
                                                   [:layout :num-fields-y]
                                                   [:layout :offset-y]
                                                   [:layout :stretch-y]
                                                   [:layout :origin :point]
                                                   [:layout :origin :offset-y]
                                                   [:layout :diagonal-mode]]
                                                  {[:layout :diagonal-mode :choices] (diagonal-mode-choices :bendy)
                                                   [:layout :origin :point :choices] position/point-choices-y})
       :bendy-sinister              (pick-options [[:line]
                                                   [:layout :num-base-fields]
                                                   [:layout :num-fields-y]
                                                   [:layout :offset-y]
                                                   [:layout :stretch-y]
                                                   [:layout :origin :point]
                                                   [:layout :origin :offset-y]
                                                   [:layout :diagonal-mode]]
                                                  {[:layout :diagonal-mode :choices] (diagonal-mode-choices :bendy)
                                                   [:layout :diagonal-mode :default] :top-right-fess
                                                   [:layout :origin :point :choices] position/point-choices-y})
       :tierced-per-pale            (pick-options [[:line]
                                                   [:layout :origin :point]
                                                   [:layout :origin :offset-x]]
                                                  {[:layout :origin :point :choices] position/point-choices-x})
       :tierced-per-fess            (pick-options [[:line]
                                                   [:layout :origin :point]
                                                   [:layout :origin :offset-y]]
                                                  {[:layout :origin :point :choices] position/point-choices-y})
       :tierced-per-pairle          (pick-options [[:line]
                                                   [:layout :origin :point]
                                                   [:layout :origin :offset-x]
                                                   [:layout :origin :offset-y]
                                                   [:layout :diagonal-mode]]
                                                  {[:layout :diagonal-mode :choices] (diagonal-mode-choices :tierced-per-pairle)
                                                   [:line :offset :min]              0})
       :tierced-per-pairle-reversed (pick-options [[:line]
                                                   [:layout :origin :point]
                                                   [:layout :origin :offset-x]
                                                   [:layout :origin :offset-y]
                                                   [:layout :diagonal-mode]]
                                                  {[:layout :diagonal-mode :choices] (diagonal-mode-choices :tierced-per-pairle-reversed)
                                                   [:layout :diagonal-mode :default] :forty-five-debrees
                                                   [:line :offset :min]              0})
       {})
     (update-in [:line] #(options/merge (line/options (get-in division [:line]))
                                        %)))))

(defn mandatory-part-count [{:keys [type] :as division}]
  (let [{:keys [num-base-fields]} (options/sanitize division (options division))]
    (if (get #{:paly :barry} type)
      num-base-fields
      (case type
        nil                          0
        :tierced-per-pale            3
        :tierced-per-fess            3
        :tierced-per-pairle          3
        :tierced-per-pairle-reversed 3
        2))))

(defn counterchangable? [division]
  ;; TODO: potentially also should look at the parts, maybe demand no
  ;; ordinaries and charges as well, but for now this check suffices
  (and (-> division mandatory-part-count (= 2))
       (-> division :fields (get 0) :division :type not)
       (-> division :fields (get 1) :division :type not)))

(defn default-fields [{:keys [type] :as division}]
  (let [{:keys [layout]}                                    (options/sanitize division (options division))
        {:keys [num-fields-x num-fields-y num-base-fields]} layout
        defaults                                            [default/field
                                                             (-> default/field
                                                                 (assoc-in [:content :tincture] :azure))
                                                             (-> default/field
                                                                 (assoc-in [:content :tincture] :sable))
                                                             (-> default/field
                                                                 (assoc-in [:content :tincture] :gules))
                                                             (-> default/field
                                                                 (assoc-in [:content :tincture] :or))
                                                             (-> default/field
                                                                 (assoc-in [:content :tincture] :vert))]]
    (into (subvec defaults 0 2)
          (cond
            (= :per-saltire type)                  [{:ref 1} {:ref 0}]
            (= :quarterly type)                    [{:ref 1} {:ref 0}]
            (= :gyronny type)                      [{:ref 1} {:ref 0} {:ref 0} {:ref 1} {:ref 1} {:ref 0}]
            (= :paly type)                         (-> []
                                                       (into (map (fn [i]
                                                                    (nth defaults (mod (+ i 2) (count defaults)))) (range (- num-base-fields 2))))
                                                       (into (map (fn [i]
                                                                    {:ref (mod i num-base-fields)}) (range (- num-fields-x num-base-fields)))))
            (= :barry type)                        (-> []
                                                       (into (map (fn [i]
                                                                    (nth defaults (mod (+ i 2) (count defaults)))) (range (- num-base-fields 2))))
                                                       (into (map (fn [i]
                                                                    {:ref (mod i num-base-fields)}) (range (- num-fields-y num-base-fields)))))
            (#{:bendy
               :bendy-sinister} type)              (-> []
                                                       (into (map (fn [i]
                                                                    (nth defaults (mod (+ i 2) (count defaults)))) (range (- num-base-fields 2))))
                                                       (into (map (fn [i]
                                                                    {:ref (mod i num-base-fields)}) (range (- num-fields-y num-base-fields)))))
            (#{:tierced-per-pale
               :tierced-per-fess
               :tierced-per-pairle
               :tierced-per-pairle-reversed} type) [(nth defaults 2)]))))

(defn get-field [fields index]
  (let [part (get fields index)
        ref  (:ref part)]
    (if ref
      (get fields ref)
      part)))

(defn division-context-key [key]
  (keyword (str "division-" (name key))))

(defn make-division [type fields parts mask-overlaps outline parent-environment parent
                     {:keys [render-field db-path transform svg-export?] :as context}]
  (let [mask-ids     (->> (range (count fields))
                          (map (fn [idx] [(util/id (str (name type) "-" idx))
                                          (util/id (str (name type) "-" idx))])))
        environments (->> parts
                          (map-indexed (fn [idx [shape-path bounding-box & extra]]
                                         (let [field (get-field fields idx)]
                                           (field-environment/create
                                            (svg/make-path shape-path)
                                            {:parent               parent
                                             :context              [type idx]
                                             :bounding-box         (svg/bounding-box bounding-box)
                                             :override-environment (when (or (:inherit-environment? field)
                                                                             (:counterchanged? field))
                                                                     parent-environment)
                                             :mask                 (first extra)}))))
                          vec)]
    [:<>
     [:defs
      (for [[idx [clip-path-id mask-id]] (map-indexed vector mask-ids)]
        (let [env               (get environments idx)
              environment-shape (:shape env)
              overlap-paths     (get mask-overlaps idx)]
          ^{:key idx}
          [:<>
           [(if svg-export?
              :mask
              :clipPath) {:id clip-path-id}
            [:path {:d    environment-shape
                    :fill "#fff"}]
            (cond
              (= overlap-paths :all) [:path {:d            environment-shape
                                             :fill         "none"
                                             :stroke-width overlap-stroke-width
                                             :stroke       "#fff"}]
              overlap-paths          (for [[idx shape] (map-indexed vector overlap-paths)]
                                       ^{:key idx}
                                       [:path {:d            shape
                                               :fill         "none"
                                               :stroke-width overlap-stroke-width
                                               :stroke       "#fff"}]))]
           (when-let [mask-shape (-> env :meta :mask)]
             [:mask {:id mask-id}
              [:path {:d    environment-shape
                      :fill "#fff"}]
              [:path {:d    mask-shape
                      :fill "#000"}]])]))]

     (for [[idx [clip-path-id mask-id]] (map-indexed vector mask-ids)]
       (let [env (get environments idx)]
         ^{:key idx}
         [:g {(if svg-export?
                :mask
                :clip-path) (str "url(#" clip-path-id ")")}
          [:g {:transform transform
               :mask      (when (-> env :meta :mask)
                            (str "url(#" mask-id ")"))}
           [render-field
            (get-field fields idx)
            (get environments idx)
            (-> context
                (assoc :db-path (if (-> type
                                        name
                                        (s/split #"-" 2)
                                        first
                                        (->> (get #{"charge" "ordinary"}))) ;; FIXME: bit of a hack
                                  (conj db-path :field)
                                  (conj db-path :fields idx))))]]]))
     outline]))

(defn paly-parts [{:keys [num-fields-x
                          offset-x
                          stretch-x]} top-left bottom-right line hints render-options]
  (let [offset-x                 (or offset-x 0)
        stretch-x                (or stretch-x 1)
        width                    (- (:x bottom-right)
                                    (:x top-left))
        pallet-width             (-> width
                                     (/ num-fields-x)
                                     (* stretch-x))
        required-width           (* pallet-width
                                    num-fields-x)
        middle                   (-> width
                                     (/ 2)
                                     (+ (:x top-left)))
        x0                       (-> middle
                                     (- (/ required-width 2))
                                     (+ (* offset-x
                                           pallet-width)))
        y1                       (:y top-left)
        y2                       (:y bottom-right)
        height                   (- y2 y1)
        {line-down :line}        (line/create line
                                              height
                                              :flipped? true
                                              :angle 90
                                              :render-options render-options)
        {line-up        :line
         line-up-length :length} (line/create line
                                              height
                                              :angle -90
                                              :reversed? true
                                              :render-options render-options)
        line-up-origin           (v/extend (v/v 0 y1) (v/v 0 y2) line-up-length)
        parts                    (->> (range num-fields-x)
                                      (map (fn [i]
                                             (let [x1         (+ x0 (* i pallet-width))
                                                   x2         (+ x1 pallet-width)
                                                   last-part? (-> i inc (= num-fields-x))]
                                               [(cond
                                                  (zero? i) ["M" [x2 y1]
                                                             (line/stitch line-down)
                                                             (infinity/path :clockwise
                                                                            [:bottom :top]
                                                                            [(v/v x2 y2) (v/v x2 y1)])
                                                             "z"]
                                                  (even? i) (cond-> ["M" [x1 (:y line-up-origin)]
                                                                     (line/stitch line-up)]
                                                              last-part?       (concat
                                                                                [(infinity/path :clockwise
                                                                                                [:top :bottom]
                                                                                                [(v/v x1 y1) (v/v x1 y2)])
                                                                                 "z"])
                                                              (not last-part?) (concat
                                                                                [(infinity/path :clockwise
                                                                                                [:top :top]
                                                                                                [(v/v x1 y1) (v/v x2 y1)])
                                                                                 "L" [x2 y1]
                                                                                 (line/stitch line-down)
                                                                                 (infinity/path :clockwise
                                                                                                [:bottom :bottom]
                                                                                                [(v/v x2 y2) (v/v x1 y2)])
                                                                                 "z"]))
                                                  :else     (cond-> ["M" [x1 y1]
                                                                     (line/stitch line-down)]
                                                              last-part?       (concat
                                                                                [(infinity/path :counter-clockwise
                                                                                                [:bottom :top]
                                                                                                [(v/v x1 y2) (v/v x1 y1)])
                                                                                 "z"])
                                                              (not last-part?) (concat
                                                                                [(infinity/path :counter-clockwise
                                                                                                [:bottom :bottom]
                                                                                                [(v/v x1 y2) (v/v x2 y2)])
                                                                                 "L" [x2 (:y line-up-origin)]
                                                                                 (line/stitch line-up)
                                                                                 (infinity/path :clockwise
                                                                                                [:top :top]
                                                                                                [(v/v x2 y1) (v/v x1 y1)])
                                                                                 "z"])))
                                                [(v/v x1 y1) (v/v x2 y2)]])))
                                      vec)
        edges                    (->> num-fields-x
                                      dec
                                      range
                                      (map (fn [i]
                                             (let [x1 (+ x0 (* i pallet-width))
                                                   x2 (+ x1 pallet-width)]
                                               (if (even? i)
                                                 (svg/make-path ["M" [x2 y1]
                                                                 (line/stitch line-down)])
                                                 (svg/make-path ["M" [x2 (:y line-up-origin)]
                                                                 (line/stitch line-up)])))))
                                      vec)
        overlap                  (-> edges
                                     (->> (map vector))
                                     vec
                                     (conj nil))
        outlines                 (when (or (:outline? render-options)
                                           (:outline? hints))
                                   [:g outline-style
                                    (for [i (range (dec num-fields-x))]
                                      ^{:key i}
                                      [:path {:d (nth edges i)}])])]
    [parts overlap outlines]))

(defn barry-parts [{:keys [num-fields-y
                           offset-y
                           stretch-y]} top-left bottom-right line hints render-options]
  (let [offset-y                   (or offset-y 0)
        stretch-y                  (or stretch-y 1)
        height                     (- (:y bottom-right)
                                      (:y top-left))
        bar-height                 (-> height
                                       (/ num-fields-y)
                                       (* stretch-y))
        required-height            (* bar-height
                                      num-fields-y)
        middle                     (-> height
                                       (/ 2)
                                       (+ (:y top-left)))
        y0                         (-> middle
                                       (- (/ required-height 2))
                                       (+ (* offset-y
                                             bar-height)))
        x1                         (:x top-left)
        x2                         (:x bottom-right)
        width                      (- x2 x1)
        {line-right :line}         (line/create line
                                                width
                                                :render-options render-options)
        {line-left        :line
         line-left-length :length} (line/create line
                                                width
                                                :flipped? true
                                                :reversed? true
                                                :render-options render-options)
        line-left-origin           (v/extend (v/v x1 0) (v/v x2 0) line-left-length)
        parts                      (->> (range num-fields-y)
                                        (map (fn [i]
                                               (let [y1         (+ y0 (* i bar-height))
                                                     y2         (+ y1 bar-height)
                                                     last-part? (-> i inc (= num-fields-y))]
                                                 [(cond
                                                    (zero? i) ["M" [x1 y2]
                                                               (line/stitch line-right)
                                                               (infinity/path :counter-clockwise
                                                                              [:right :left]
                                                                              [(v/v x2 y2) (v/v x1 y2)])
                                                               "z"]
                                                    (even? i) (cond-> ["M" [(:x line-left-origin) y1]
                                                                       (line/stitch line-left)]
                                                                last-part?       (concat
                                                                                  [(infinity/path :counter-clockwise
                                                                                                  [:left :right]
                                                                                                  [(v/v x1 y1) (v/v x2 y1)])
                                                                                   "z"])
                                                                (not last-part?) (concat
                                                                                  [(infinity/path :counter-clockwise
                                                                                                  [:left :left]
                                                                                                  [(v/v x1 y1) (v/v x1 y2)])
                                                                                   "L" [x1 y2]
                                                                                   (line/stitch line-right)
                                                                                   (infinity/path :clockwise
                                                                                                  [:right :right]
                                                                                                  [(v/v x2 y2) (v/v x2 y1)])
                                                                                   "z"]))
                                                    :else     (cond-> ["M" [x1 y1]
                                                                       (line/stitch line-right)]
                                                                last-part?       (concat
                                                                                  [(infinity/path :clockwise
                                                                                                  [:right :left]
                                                                                                  [(v/v x2 y1) (v/v x1 y1)])
                                                                                   "z"])
                                                                (not last-part?) (concat
                                                                                  [(infinity/path :clockwise
                                                                                                  [:right :right]
                                                                                                  [(v/v x2 y1) (v/v x1 y2)])
                                                                                   "L" [(:x line-left-origin) y2]
                                                                                   (line/stitch line-left)
                                                                                   (infinity/path :clockwise
                                                                                                  [:left :left]
                                                                                                  [(v/v x1 y2) (v/v x1 y1)])
                                                                                   "z"])))
                                                  [(v/v x1 y1) (v/v x2 y2)]])))
                                        vec)
        edges                      (->> num-fields-y
                                        dec
                                        range
                                        (map (fn [i]
                                               (let [y1 (+ y0 (* i bar-height))
                                                     y2 (+ y1 bar-height)]
                                                 (if (even? i)
                                                   (svg/make-path ["M" [x1 y2]
                                                                   (line/stitch line-right)])
                                                   (svg/make-path ["M" [(:x line-left-origin) y2]
                                                                   (line/stitch line-left)])))))
                                        vec)
        overlap                    (-> edges
                                       (->> (map vector))
                                       vec
                                       (conj nil))
        outlines                   (when (or (:outline? render-options)
                                             (:outline? hints))
                                     [:g outline-style
                                      (for [i (range (dec num-fields-y))]
                                        ^{:key i}
                                        [:path {:d (nth edges i)}])])]
    [parts overlap outlines]))

(defn per-pale
  {:display-name "Per pale"
   :parts        ["dexter" "sinister"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line layout]}      (options/sanitize division (options division))
        {:keys [origin]}           layout
        points                     (:points environment)
        origin-point               (position/calculate origin environment :fess)
        top-left                   (:top-left points)
        top                        (assoc (:top points) :x (:x origin-point))
        bottom                     (assoc (:bottom points) :x (:x origin-point))
        bottom-right               (:bottom-right points)
        {line-one    :line
         line-length :line-length} (line/create line
                                                (:y (v/- bottom top))
                                                :angle 90
                                                :reversed? true
                                                :render-options render-options)
        bottom-adjusted            (v/extend top bottom line-length)
        parts                      [[["M" bottom-adjusted
                                      (line/stitch line-one)
                                      (infinity/path :clockwise
                                                     [:bottom :top]
                                                     [bottom-adjusted top])
                                      "z"]
                                     [top-left bottom]]

                                    [["M" bottom-adjusted
                                      (line/stitch line-one)
                                      (infinity/path :counter-clockwise
                                                     [:bottom :top]
                                                     [bottom-adjusted top])
                                      "z"]
                                     [top bottom-right]]]]
    [make-division
     (division-context-key type) fields parts
     [:all nil]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline-style
        [:path {:d (svg/make-path
                    ["M" bottom-adjusted
                     (line/stitch line-one)])}]])

     environment division context]))

(defn per-fess
  {:display-name "Per fess"
   :parts        ["chief" "base"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line layout]} (options/sanitize division (options division))
        {:keys [origin]}      layout
        points                (:points environment)
        origin-point          (position/calculate origin environment :fess)
        top-left              (:top-left points)
        left                  (assoc (:left points) :y (:y origin-point))
        right                 (assoc (:right points) :y (:y origin-point))
        bottom-right          (:bottom-right points)
        {line-one :line}      (line/create line
                                           (:x (v/- right left))
                                           :render-options render-options)
        parts                 [[["M" left
                                 (line/stitch line-one)
                                 (infinity/path :counter-clockwise
                                                [:right :left]
                                                [right left])
                                 "z"]
                                [top-left right]]

                               [["M" left
                                 (line/stitch line-one)
                                 (infinity/path :clockwise
                                                [:right :left]
                                                [right left])
                                 "z"]
                                [left bottom-right]]]]
    [make-division
     (division-context-key type) fields parts
     [:all nil]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline-style
        [:path {:d (svg/make-path
                    ["M" left
                     (line/stitch line-one)])}]])
     environment division context]))

(defn angle-to-point [p1 p2]
  (let [d         (v/- p2 p1)
        angle-rad (Math/atan2 (:y d) (:x d))]
    (-> angle-rad
        (/ Math/PI)
        (* 180))))

(defn direction [diagonal-mode points & [origin]]
  (let [top-left      (:top-left points)
        top-right     (:top-right points)
        bottom-left   (:bottom-left points)
        bottom-right  (:bottom-right points)
        origin        (or origin (:fess points))
        origin-height (-> origin
                          (v/- top-left)
                          :y)
        dir           (case diagonal-mode
                        :top-left-fess     (v/- origin top-left)
                        :top-right-fess    (v/- origin top-right)
                        :bottom-left-fess  (v/- origin bottom-left)
                        :bottom-right-fess (v/- origin bottom-right)
                        (v/v origin-height origin-height))]
    (v/v (-> dir :x Math/abs)
         (-> dir :y Math/abs))))

(defn per-bend
  {:display-name "Per bend"
   :parts        ["chief" "base"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line layout]}          (options/sanitize division (options division))
        {:keys [origin diagonal-mode]} layout
        points                         (:points environment)
        origin-point                   (position/calculate origin environment :fess)
        top-left                       (:top-left points)
        top                            (:top points)
        bottom                         (:bottom points)
        left                           (:left points)
        right                          (:right points)
        direction                      (direction diagonal-mode points origin-point)
        diagonal-start                 (v/project-x origin-point (v/dot direction (v/v -1 -1)) (:x left))
        diagonal-end                   (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        angle                          (angle-to-point diagonal-start diagonal-end)
        {line-one :line}               (line/create line
                                                    (v/abs (v/- diagonal-end diagonal-start))
                                                    :angle angle
                                                    :render-options render-options)
        parts                          [[["M" diagonal-start
                                          (line/stitch line-one)
                                          (infinity/path :counter-clockwise
                                                         [:right :top]
                                                         [diagonal-end diagonal-start])
                                          "z"]
                                         [diagonal-start top diagonal-end]]
                                        [["M" diagonal-start
                                          (line/stitch line-one)
                                          (infinity/path :clockwise
                                                         [:right :top]
                                                         [diagonal-end diagonal-start])
                                          "z"]
                                         [diagonal-start diagonal-end bottom]]]]
    [make-division
     (division-context-key type) fields parts
     [:all nil]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline-style
        [:path {:d (svg/make-path
                    ["M" top-left
                     (line/stitch line-one)])}]])
     environment division context]))

(defn per-bend-sinister
  {:display-name "Per bend sinister"
   :parts        ["chief" "base"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line layout]}          (options/sanitize division (options division))
        {:keys [origin diagonal-mode]} layout
        points                         (:points environment)
        origin-point                   (position/calculate origin environment :fess)
        top                            (:top points)
        bottom                         (:bottom points)
        left                           (:left points)
        right                          (:right points)
        direction                      (direction diagonal-mode points origin-point)
        diagonal-start                 (v/project-x origin-point (v/dot direction (v/v 1 -1)) (:x right))
        diagonal-end                   (v/project-x origin-point (v/dot direction (v/v -1 1)) (:x left))
        angle                          (angle-to-point diagonal-start diagonal-end)
        {line-one    :line
         line-length :length}          (line/create line
                                                    (v/abs (v/- diagonal-end diagonal-start))
                                                    :angle (+ angle 180)
                                                    :reversed? true
                                                    :render-options render-options)
        diagonal-end-adjusted          (v/extend
                                           diagonal-start
                                         diagonal-end
                                         line-length)
        parts                          [[["M" diagonal-end-adjusted
                                          (line/stitch line-one)
                                          (infinity/path :counter-clockwise
                                                         [:top :left]
                                                         [diagonal-start diagonal-end])
                                          "z"]
                                         [diagonal-start top diagonal-end]]

                                        [["M" diagonal-end-adjusted
                                          (line/stitch line-one)
                                          (infinity/path :clockwise
                                                         [:top :left]
                                                         [diagonal-start diagonal-end])
                                          "z"]
                                         [diagonal-start bottom diagonal-end]]]]
    [make-division
     (division-context-key type) fields parts
     [:all nil]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline-style
        [:path {:d (svg/make-path
                    ["M" diagonal-end-adjusted
                     (line/stitch line-one)])}]])
     environment division context]))

(defn per-chevron
  {:display-name "Per chevron"
   :parts        ["chief" "base"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line layout]}          (options/sanitize division (options division))
        {:keys [origin diagonal-mode]} layout
        points                         (:points environment)
        origin-point                   (position/calculate origin environment :fess)
        top-left                       (:top-left points)
        bottom-left                    (:bottom-left points)
        bottom-right                   (:bottom-right points)
        left                           (:left points)
        right                          (:right points)
        direction                      (direction diagonal-mode points origin-point)
        diagonal-bottom-left           (v/project-x origin-point (v/dot direction (v/v -1 1)) (:x left))
        diagonal-bottom-right          (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        angle-bottom-left              (angle-to-point origin-point diagonal-bottom-left)
        angle-bottom-right             (angle-to-point origin-point diagonal-bottom-right)
        {line-left        :line
         line-left-length :length}     (line/create line
                                                    (v/abs (v/- diagonal-bottom-left origin-point))
                                                    :angle (+ angle-bottom-left 180)
                                                    :reversed? true
                                                    :render-options render-options)
        {line-right :line}             (line/create line
                                                    (v/abs (v/- diagonal-bottom-right origin-point))
                                                    :angle angle-bottom-right
                                                    :render-options render-options)
        diagonal-bottom-left-adjusted  (v/extend origin-point diagonal-bottom-left line-left-length)
        parts                          [[["M" diagonal-bottom-left-adjusted
                                          (line/stitch line-left)
                                          "L" origin-point
                                          (line/stitch line-right)
                                          (infinity/path :counter-clockwise
                                                         [:right :left]
                                                         [diagonal-bottom-right diagonal-bottom-left])
                                          "z"]
                                         [top-left bottom-right]]

                                        [["M" diagonal-bottom-left-adjusted
                                          (line/stitch line-left)
                                          "L" origin-point
                                          (line/stitch line-right)
                                          (infinity/path :clockwise
                                                         [:right :left]
                                                         [diagonal-bottom-right diagonal-bottom-left])
                                          "z"
                                          "z"]
                                         [bottom-left origin-point bottom-right]]]]
    [make-division
     (division-context-key type) fields parts
     [:all nil]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline-style
        [:path {:d (svg/make-path
                    ["M" diagonal-bottom-left-adjusted
                     (line/stitch line-left)
                     "L" origin-point
                     (line/stitch line-right)])}]])
     environment division context]))

(defn per-saltire
  {:display-name "Per saltire"
   :parts        ["chief" "dexter" "sinister" "base"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line layout]}              (options/sanitize division (options division))
        {:keys [origin diagonal-mode]}     layout
        points                             (:points environment)
        origin-point                       (position/calculate origin environment :fess)
        top-left                           (:top-left points)
        top-right                          (:top-right points)
        bottom-left                        (:bottom-left points)
        bottom-right                       (:bottom-right points)
        left                               (:left points)
        right                              (:right points)
        direction                          (direction diagonal-mode points origin-point)
        diagonal-top-left                  (v/project-x origin-point (v/dot direction (v/v -1 -1)) (:x left))
        diagonal-top-right                 (v/project-x origin-point (v/dot direction (v/v 1 -1)) (:x right))
        diagonal-bottom-left               (v/project-x origin-point (v/dot direction (v/v -1 1)) (:x left))
        diagonal-bottom-right              (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        angle-top-left                     (angle-to-point origin-point diagonal-top-left)
        angle-top-right                    (angle-to-point origin-point diagonal-top-right)
        angle-bottom-left                  (angle-to-point origin-point diagonal-bottom-left)
        angle-bottom-right                 (angle-to-point origin-point diagonal-bottom-right)
        {line-top-left        :line
         line-top-left-length :length}     (line/create line
                                                        (v/abs (v/- diagonal-top-left origin-point))
                                                        :angle (+ angle-top-left 180)
                                                        :reversed? true
                                                        :render-options render-options)
        {line-top-right :line}             (line/create line
                                                        (v/abs (v/- diagonal-top-right origin-point))
                                                        :angle angle-top-right
                                                        :flipped? true
                                                        :render-options render-options)
        {line-bottom-right        :line
         line-bottom-right-length :length} (line/create line
                                                        (v/abs (v/- diagonal-bottom-right origin-point))
                                                        :angle (+ angle-bottom-right 180)
                                                        :reversed? true
                                                        :render-options render-options)
        {line-bottom-left :line}           (line/create line
                                                        (v/abs (v/- diagonal-bottom-left origin-point))
                                                        :angle angle-bottom-left
                                                        :flipped? true
                                                        :render-options render-options)
        diagonal-top-left-adjusted         (v/extend
                                               origin-point
                                             diagonal-top-left
                                             line-top-left-length)
        diagonal-bottom-right-adjusted     (v/extend
                                               origin-point
                                             diagonal-bottom-right
                                             line-bottom-right-length)
        parts                              [[["M" diagonal-top-left-adjusted
                                              (line/stitch line-top-left)
                                              "L" origin-point
                                              (line/stitch line-top-right)
                                              (infinity/path :counter-clockwise
                                                             [:right :left]
                                                             [diagonal-top-right diagonal-top-left])
                                              "z"]
                                             [top-left origin-point top-right]]

                                            [["M" diagonal-top-left-adjusted
                                              (line/stitch line-top-left)
                                              "L" origin-point
                                              (line/stitch line-bottom-left)
                                              (infinity/path :clockwise
                                                             [:left :left]
                                                             [diagonal-bottom-left diagonal-top-left-adjusted])
                                              "z"]
                                             [diagonal-top-left origin-point diagonal-bottom-left]]

                                            [["M" diagonal-bottom-right-adjusted
                                              (line/stitch line-bottom-right)
                                              "L" origin-point
                                              (line/stitch line-top-right)
                                              (infinity/path :clockwise
                                                             [:right :right]
                                                             [diagonal-top-right diagonal-bottom-right])
                                              "z"]
                                             [diagonal-top-right origin-point diagonal-bottom-right]]

                                            [["M" diagonal-bottom-right-adjusted
                                              (line/stitch line-bottom-right)
                                              "L" origin-point
                                              (line/stitch line-bottom-left)
                                              (infinity/path :counter-clockwise
                                                             [:left :right]
                                                             [diagonal-bottom-left diagonal-bottom-right-adjusted])
                                              "z"]
                                             [bottom-left origin-point bottom-right]]]]

    [make-division
     (division-context-key type) fields parts
     [:all
      [(svg/make-path
        ["M" origin-point
         (line/stitch line-bottom-left)])]
      [(svg/make-path
        ["M" diagonal-bottom-right-adjusted
         (line/stitch line-bottom-right)])]
      nil]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline-style
        [:path {:d (svg/make-path
                    ["M" diagonal-top-left-adjusted
                     (line/stitch line-top-left)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-top-right)])}]
        [:path {:d (svg/make-path
                    ["M" diagonal-bottom-right-adjusted
                     (line/stitch line-bottom-right)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-bottom-left)])}]])
     environment division context]))

(defn quarterly
  {:display-name "Quarterly"
   :parts        ["I" "II" "III" "IV"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line layout]}        (options/sanitize division (options division))
        {:keys [origin]}             layout
        points                       (:points environment)
        origin-point                 (position/calculate origin environment :fess)
        top                          (assoc (:top points) :x (:x origin-point))
        top-left                     (:top-left points)
        top-right                    (:top-right points)
        bottom                       (assoc (:bottom points) :x (:x origin-point))
        bottom-left                  (:bottom-left points)
        bottom-right                 (:bottom-right points)
        left                         (assoc (:left points) :y (:y origin-point))
        right                        (assoc (:right points) :y (:y origin-point))
        {line-top        :line
         line-top-length :length}    (line/create line
                                                  (v/abs (v/- top origin-point))
                                                  :angle 90
                                                  :reversed? true
                                                  :render-options render-options)
        {line-right :line}           (line/create line
                                                  (v/abs (v/- right origin-point))
                                                  :flipped? true
                                                  :render-options render-options)
        {line-bottom        :line
         line-bottom-length :length} (line/create line
                                                  (v/abs (v/- bottom origin-point))
                                                  :angle -90
                                                  :reversed? true
                                                  :render-options render-options)
        {line-left :line}            (line/create line
                                                  (v/abs (v/- left origin-point))
                                                  :angle -180
                                                  :flipped? true
                                                  :render-options render-options)
        top-adjusted                 (v/extend origin-point top line-top-length)
        bottom-adjusted              (v/extend origin-point bottom line-bottom-length)
        parts                        [[["M" top-adjusted
                                        (line/stitch line-top)
                                        "L" origin-point
                                        (line/stitch line-left)
                                        (infinity/path :clockwise
                                                       [:left :top]
                                                       [left top])
                                        "z"]
                                       [top-left origin-point]]

                                      [["M" top-adjusted
                                        (line/stitch line-top)
                                        "L" origin-point
                                        (line/stitch line-right)
                                        (infinity/path :counter-clockwise
                                                       [:right :top]
                                                       [right top])
                                        "z"]
                                       [origin-point top-right]]

                                      [["M" bottom-adjusted
                                        (line/stitch line-bottom)
                                        "L" origin-point
                                        (line/stitch line-left)
                                        (infinity/path :counter-clockwise
                                                       [:left :bottom]
                                                       [left bottom])
                                        "z"]
                                       [origin-point bottom-left]]

                                      [["M" bottom-adjusted
                                        (line/stitch line-bottom)
                                        "L" origin-point
                                        (line/stitch line-right)
                                        (infinity/path :clockwise
                                                       [:right :bottom]
                                                       [right bottom])
                                        "z"]
                                       [origin-point bottom-right]]]]
    [make-division
     (division-context-key type) fields parts
     [:all
      [(svg/make-path
        ["M" origin-point
         (line/stitch line-right)])]
      [(svg/make-path
        ["M" bottom-adjusted
         (line/stitch line-bottom)])]
      nil]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline-style
        [:path {:d (svg/make-path
                    ["M" top-adjusted
                     (line/stitch line-top)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-right)])}]
        [:path {:d (svg/make-path
                    ["M" bottom-adjusted
                     (line/stitch line-bottom)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-left)])}]])
     environment division context]))

(defn gyronny
  {:display-name "Gyronny"
   :parts        ["I" "II" "III" "IV" "V" "VI" "VII" "VIII"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line layout]}          (options/sanitize division (options division))
        {:keys [origin diagonal-mode]} layout
        points                         (:points environment)
        origin-point                   (position/calculate origin environment :fess)
        top                            (assoc (:top points) :x (:x origin-point))
        bottom                         (assoc (:bottom points) :x (:x origin-point))
        left                           (assoc (:left points) :y (:y origin-point))
        right                          (assoc (:right points) :y (:y origin-point))
        direction                      (direction diagonal-mode points origin-point)
        diagonal-top-left              (v/project-x origin-point (v/dot direction (v/v -1 -1)) (:x left))
        diagonal-top-right             (v/project-x origin-point (v/dot direction (v/v 1 -1)) (:x right))
        diagonal-bottom-left           (v/project-x origin-point (v/dot direction (v/v -1 1)) (:x left))
        diagonal-bottom-right          (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        angle-top-left                 (angle-to-point origin-point diagonal-top-left)
        angle-top-right                (angle-to-point origin-point diagonal-top-right)
        angle-bottom-left              (angle-to-point origin-point diagonal-bottom-left)
        angle-bottom-right             (angle-to-point origin-point diagonal-bottom-right)
        {line-top        :line
         line-top-length :length}      (line/create line
                                                    (v/abs (v/- top origin-point))
                                                    :angle 90
                                                    :reversed? true
                                                    :render-options render-options)
        {line-right        :line
         line-right-length :length}    (line/create line
                                                    (v/abs (v/- right origin-point))
                                                    :reversed? true
                                                    :angle 180
                                                    :render-options render-options)
        {line-bottom        :line
         line-bottom-length :length}   (line/create line
                                                    (v/abs (v/- bottom origin-point))
                                                    :angle -90
                                                    :reversed? true
                                                    :render-options render-options)
        {line-left        :line
         line-left-length :length}     (line/create line
                                                    (v/abs (v/- left origin-point))
                                                    :reversed? true
                                                    :render-options render-options)
        top-adjusted                   (v/extend origin-point top line-top-length)
        bottom-adjusted                (v/extend origin-point bottom line-bottom-length)
        left-adjusted                  (v/extend origin-point left line-left-length)
        right-adjusted                 (v/extend origin-point right line-right-length)
        {line-top-left :line}          (line/create line
                                                    (v/abs (v/- diagonal-top-left origin-point))
                                                    :flipped? true
                                                    :angle angle-top-left
                                                    :render-options render-options)
        {line-top-right :line}         (line/create line
                                                    (v/abs (v/- diagonal-top-right origin-point))
                                                    :flipped? true
                                                    :angle angle-top-right
                                                    :render-options render-options)
        {line-bottom-right :line}      (line/create line
                                                    (v/abs (v/- diagonal-bottom-right origin-point))
                                                    :flipped? true
                                                    :angle angle-bottom-right
                                                    :render-options render-options)
        {line-bottom-left :line}       (line/create line
                                                    (v/abs (v/- diagonal-bottom-left origin-point))
                                                    :flipped? true
                                                    :angle angle-bottom-left
                                                    :render-options render-options)
        parts                          [[["M" top-adjusted
                                          (line/stitch line-top)
                                          "L" origin-point
                                          (line/stitch line-top-left)
                                          (infinity/path :clockwise
                                                         [:left :top]
                                                         [diagonal-top-left top])
                                          "z"]
                                         [diagonal-top-left origin-point top]]

                                        [["M" top-adjusted
                                          (line/stitch line-top)
                                          "L" origin-point
                                          (line/stitch line-top-right)
                                          (infinity/path :counter-clockwise
                                                         [:right :top]
                                                         [diagonal-top-right top])
                                          "z"]
                                         [top origin-point diagonal-top-right]]

                                        [["M" left-adjusted
                                          (line/stitch line-left)
                                          "L" origin-point
                                          (line/stitch line-top-left)
                                          (infinity/path :counter-clockwise
                                                         [:left :left]
                                                         [diagonal-top-left left])
                                          "z"]
                                         [left origin-point diagonal-top-left]]

                                        [["M" right-adjusted
                                          (line/stitch line-right)
                                          "L" origin-point
                                          (line/stitch line-top-right)
                                          (infinity/path :clockwise
                                                         [:right :right]
                                                         [diagonal-top-right right])
                                          "z"]
                                         [diagonal-top-right origin-point right]]

                                        [["M" left-adjusted
                                          (line/stitch line-left)
                                          "L" origin-point
                                          (line/stitch line-bottom-left)
                                          (infinity/path :clockwise
                                                         [:left :left]
                                                         [diagonal-bottom-left left])
                                          "z"]
                                         [diagonal-bottom-left origin-point left]]

                                        [["M" right-adjusted
                                          (line/stitch line-right)
                                          "L" origin-point
                                          (line/stitch line-bottom-right)
                                          (infinity/path :counter-clockwise
                                                         [:right :right]
                                                         [diagonal-bottom-right right])
                                          "z"]
                                         [right origin-point diagonal-bottom-right]]

                                        [["M" bottom-adjusted
                                          (line/stitch line-bottom)
                                          "L" origin-point
                                          (line/stitch line-bottom-left)
                                          (infinity/path :counter-clockwise
                                                         [:left :bottom]
                                                         [diagonal-bottom-left bottom])
                                          "z"]
                                         [bottom origin-point diagonal-bottom-left]]

                                        [["M" bottom-adjusted
                                          (line/stitch line-bottom)
                                          "L" origin-point
                                          (line/stitch line-bottom-right)
                                          (infinity/path :clockwise
                                                         [:right :bottom]
                                                         [diagonal-bottom-right bottom])
                                          "z"]
                                         [diagonal-bottom-right origin-point bottom]]]]

    [make-division
     (division-context-key type) fields parts
     [:all
      [(svg/make-path
        ["M" origin-point
         (line/stitch line-top-right)])]
      [(svg/make-path
        ["M" left-adjusted
         (line/stitch line-left)])]
      [(svg/make-path
        ["M" right-adjusted
         (line/stitch line-right)])]
      [(svg/make-path
        ["M" origin-point
         (line/stitch line-bottom-left)])]
      [(svg/make-path
        ["M" origin-point
         (line/stitch line-bottom-right)])]

      [(svg/make-path
        ["M" bottom-adjusted
         (line/stitch line-bottom)])]
      nil]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline-style
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-top-left)])}]
        [:path {:d (svg/make-path
                    ["M" top-adjusted
                     (line/stitch line-top)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-top-right)])}]
        [:path {:d (svg/make-path
                    ["M" right-adjusted
                     (line/stitch line-right)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-bottom-right)])}]
        [:path {:d (svg/make-path
                    ["M" bottom-adjusted
                     (line/stitch line-bottom)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-bottom-left)])}]
        [:path {:d (svg/make-path
                    ["M" left-adjusted
                     (line/stitch line-left)])}]])
     environment division context]))

(defn paly
  {:display-name "Paly"
   :parts        []}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line layout]}    (options/sanitize division (options division))
        points                   (:points environment)
        top-left                 (:top-left points)
        bottom-right             (:bottom-right points)
        [parts overlap outlines] (paly-parts layout top-left bottom-right line hints render-options)]
    [make-division
     (division-context-key type) fields parts
     overlap
     outlines
     environment division context]))

(defn barry
  {:display-name "Barry"
   :parts        []}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line layout]}    (options/sanitize division (options division))
        points                   (:points environment)
        top-left                 (:top-left points)
        bottom-right             (:bottom-right points)
        [parts overlap outlines] (barry-parts layout top-left bottom-right line hints render-options)]
    [make-division
     (division-context-key type) fields parts
     overlap
     outlines
     environment division context]))

(defn bendy
  {:display-name "Bendy"
   :parts        []}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line layout]}    (options/sanitize division (options division))
        points                   (:points environment)
        top-left                 (:top-left points)
        top-right                (:top-right points)
        origin-point             (position/calculate (:origin layout) environment :fess)
        direction                (direction (:diagonal-mode layout) points origin-point)
        direction-orthogonal     (v/v (-> direction :y) (-> direction :x -))
        angle                    (angle-to-point (v/v 0 0) direction)
        required-half-width      (v/distance-point-to-line top-left origin-point (v/+ origin-point direction-orthogonal))
        required-half-height     (v/distance-point-to-line top-right origin-point (v/+ origin-point direction))
        [parts overlap outlines] (barry-parts layout
                                              (v/v (- required-half-width) (- required-half-height))
                                              (v/v required-half-width required-half-height)
                                              line hints render-options)]
    [:g {:transform (str "translate(" (:x origin-point) "," (:y origin-point) ")"
                         "rotate(" angle ")")}
     [make-division
      (division-context-key type) fields parts
      overlap
      outlines
      environment division context]]))

(defn bendy-sinister
  {:display-name "Bendy sinister"
   :parts        []}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line layout]}    (options/sanitize division (options division))
        points                   (:points environment)
        top-left                 (:top-left points)
        top-right                (:top-right points)
        origin-point             (position/calculate (:origin layout) environment :fess)
        direction                (direction (:diagonal-mode layout) points origin-point)
        direction-orthogonal     (v/v (-> direction :y) (-> direction :x -))
        angle                    (angle-to-point (v/v 0 0) (v/dot direction (v/v 1 -1)))
        required-half-width      (v/distance-point-to-line top-right origin-point (v/+ origin-point direction))
        required-half-height     (v/distance-point-to-line top-left origin-point (v/+ origin-point direction-orthogonal))
        [parts overlap outlines] (barry-parts layout
                                              (v/v (- required-half-width) (- required-half-height))
                                              (v/v required-half-width required-half-height)
                                              line hints render-options)]
    [:g {:transform (str "translate(" (:x origin-point) "," (:y origin-point) ")"
                         "rotate(" angle ")")}
     [make-division
      (division-context-key type) fields parts
      overlap
      outlines
      environment division context]]))

(defn tierced-per-pale
  {:display-name "Tierced per pale"
   :parts        ["dexter" "fess" "sinister"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line layout]}          (options/sanitize division (options division))
        {:keys [origin]}               layout
        points                         (:points environment)
        origin-point                   (position/calculate origin environment :fess)
        top                            (assoc (:top points) :x (:x origin-point))
        top-left                       (:top-left points)
        bottom                         (assoc (:bottom points) :x (:x origin-point))
        bottom-right                   (:bottom-right points)
        width                          (:width environment)
        col1                           (- (:x origin-point) (/ width 6))
        col2                           (+ (:x origin-point) (/ width 6))
        first-top                      (v/v col1 (:y top))
        first-bottom                   (v/v col1 (:y bottom))
        second-top                     (v/v col2 (:y top))
        second-bottom                  (v/v col2 (:y bottom))
        {line-one :line}               (line/create line
                                                    (:y (v/- bottom top))
                                                    :flipped? true
                                                    :angle 90
                                                    :render-options render-options)
        {line-reversed        :line
         line-reversed-length :length} (line/create line
                                                    (:y (v/- bottom top))
                                                    :angle -90
                                                    :reversed? true
                                                    :render-options render-options)
        second-bottom-adjusted         (v/extend second-top second-bottom line-reversed-length)
        parts                          [[["M" first-top
                                          (line/stitch line-one)
                                          (infinity/path :clockwise
                                                         [:bottom :top]
                                                         [first-bottom first-top])
                                          "z"]
                                         [top-left first-bottom]]

                                        [["M" second-bottom-adjusted
                                          (line/stitch line-reversed)
                                          (infinity/path :counter-clockwise
                                                         [:top :top]
                                                         [second-top first-top])
                                          (line/stitch line-one)
                                          (infinity/path :counter-clockwise
                                                         [:bottom :bottom]
                                                         [first-bottom second-bottom])
                                          "z"]
                                         [first-top second-bottom]]

                                        [["M" second-bottom-adjusted
                                          (line/stitch line-reversed)
                                          (infinity/path :clockwise
                                                         [:top :bottom]
                                                         [second-top second-bottom])
                                          "z"]
                                         [second-top bottom-right]]]]
    [make-division
     (division-context-key type) fields parts
     [:all
      [(svg/make-path
        ["M" second-bottom-adjusted
         (line/stitch line-reversed)])]
      nil]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline-style
        [:path {:d (svg/make-path
                    ["M" first-top
                     (line/stitch line-one)])}]
        [:path {:d (svg/make-path
                    ["M" second-bottom-adjusted
                     (line/stitch line-reversed)])}]])
     environment division context]))

(defn tierced-per-fess
  {:display-name "Tierced per fess"
   :parts        ["chief" "fess" "base"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line layout]}          (options/sanitize division (options division))
        {:keys [origin]}               layout
        points                         (:points environment)
        origin-point                   (position/calculate origin environment :fess)
        top-left                       (:top-left points)
        bottom-right                   (:bottom-right points)
        left                           (assoc (:left points) :y (:y origin-point))
        right                          (assoc (:right points) :y (:y origin-point))
        height                         (:height environment)
        row1                           (- (:y origin-point) (/ height 6))
        row2                           (+ (:y origin-point) (/ height 6))
        first-left                     (v/v (:x left) row1)
        first-right                    (v/v (:x right) row1)
        second-left                    (v/v (:x left) row2)
        second-right                   (v/v (:x right) row2)
        {line-one :line}               (line/create line
                                                    (:x (v/- right left))
                                                    :render-options render-options)
        {line-reversed        :line
         line-reversed-length :length} (line/create line
                                                    (:x (v/- right left))
                                                    :reversed? true
                                                    :flipped? true
                                                    :angle 180
                                                    :render-options render-options)
        second-right-adjusted          (v/extend second-left second-right line-reversed-length)
        parts                          [[["M" first-left
                                          (line/stitch line-one)
                                          (infinity/path :counter-clockwise
                                                         [:right :left]
                                                         [first-right first-left])
                                          "z"]
                                         [top-left first-right]]

                                        [["M" first-left
                                          (line/stitch line-one)
                                          (infinity/path :clockwise
                                                         [:right :right]
                                                         [first-right second-right-adjusted])
                                          (line/stitch line-reversed)
                                          (infinity/path :clockwise
                                                         [:left :left]
                                                         [second-left first-left])
                                          "z"]
                                         [first-left second-right]]

                                        [["M" second-right-adjusted
                                          (line/stitch line-reversed)
                                          (infinity/path :counter-clockwise
                                                         [:left :right]
                                                         [second-left second-right-adjusted])
                                          "z"]
                                         [second-left bottom-right]]]]
    [make-division
     (division-context-key type) fields parts
     [:all
      [(svg/make-path
        ["M" second-right-adjusted
         (line/stitch line-reversed)])]
      nil]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline-style
        [:path {:d (svg/make-path
                    ["M" first-left
                     (line/stitch line-one)])}]
        [:path {:d (svg/make-path
                    ["M" second-right-adjusted
                     (line/stitch line-reversed)])}]])
     environment division context]))

(defn tierced-per-pairle
  {:display-name "Tierced per pairle"
   :parts        ["chief" "dexter" "sinister"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line layout]}                 (options/sanitize division (options division))
        {:keys [origin diagonal-mode]}        layout
        points                                (:points environment)
        origin-point                          (position/calculate origin environment :fess)
        bottom                                (assoc (:bottom points) :x (:x origin-point))
        bottom-left                           (:bottom-left points)
        bottom-right                          (:bottom-right points)
        left                                  (assoc (:left points) :y (:y origin-point))
        right                                 (assoc (:right points) :y (:y origin-point))
        direction                             (direction diagonal-mode points origin-point)
        diagonal-top-left                     (v/project-x origin-point (v/dot direction (v/v -1 -1)) (:x left))
        diagonal-top-right                    (v/project-x origin-point (v/dot direction (v/v 1 -1)) (:x right))
        angle-top-left                        (angle-to-point origin-point diagonal-top-left)
        angle-top-right                       (angle-to-point origin-point diagonal-top-right)
        {line-top-left        :line
         line-top-left-length :length}        (line/create line
                                                           (v/abs (v/- diagonal-top-left origin-point))
                                                           :angle (+ angle-top-left 180)
                                                           :reversed? true
                                                           :render-options render-options)
        {line-top-right :line}                (line/create line
                                                           (v/abs (v/- diagonal-top-right origin-point))
                                                           :angle angle-top-right
                                                           :flipped? true
                                                           :render-options render-options)
        {line-bottom :line}                   (line/create line
                                                           (v/abs (v/- bottom origin-point))
                                                           :flipped? true
                                                           :angle 90
                                                           :render-options render-options)
        {line-bottom-reversed        :line
         line-bottom-reversed-length :length} (line/create line
                                                           (v/abs (v/- bottom origin-point))
                                                           :angle -90
                                                           :reversed? true
                                                           :render-options render-options)
        diagonal-top-left-adjusted            (v/extend
                                                  origin-point
                                                diagonal-top-left
                                                line-top-left-length)
        bottom-adjusted                       (v/extend
                                                  origin-point
                                                bottom
                                                line-bottom-reversed-length)
        parts                                 [[["M" diagonal-top-left-adjusted
                                                 (line/stitch line-top-left)
                                                 "L" origin-point
                                                 (line/stitch line-top-right)
                                                 (infinity/path :counter-clockwise
                                                                [:right :left]
                                                                [diagonal-top-right diagonal-top-left])
                                                 "z"]
                                                [diagonal-top-left origin-point diagonal-top-right]]

                                               [["M" bottom-adjusted
                                                 (line/stitch line-bottom-reversed)
                                                 "L" origin-point
                                                 (line/stitch line-top-right)
                                                 (infinity/path :clockwise
                                                                [:right :bottom]
                                                                [diagonal-top-right bottom])
                                                 "z"]
                                                [origin-point diagonal-top-right bottom-right bottom]]

                                               [["M" diagonal-top-left-adjusted
                                                 (line/stitch line-top-left)
                                                 "L" origin-point
                                                 (line/stitch line-bottom)
                                                 (infinity/path :clockwise
                                                                [:bottom :left]
                                                                [bottom diagonal-top-left])
                                                 "z"]
                                                [diagonal-top-left origin-point bottom bottom-left]]]]
    [make-division
     (division-context-key type) fields parts
     [:all
      [(svg/make-path
        ["M" bottom-adjusted
         (line/stitch line-bottom-reversed)])]
      nil]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline-style
        [:path {:d (svg/make-path
                    ["M" diagonal-top-left-adjusted
                     (line/stitch line-top-left)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-top-right)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-bottom)])}]])
     environment division context]))

(defn tierced-per-pairle-reversed
  {:display-name "Tierced per pairle reversed"
   :parts        ["dexter" "sinister" "base"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line layout]}              (options/sanitize division (options division))
        {:keys [origin diagonal-mode]}     layout
        points                             (:points environment)
        origin-point                       (position/calculate origin environment :fess)
        top                                (assoc (:top points) :x (:x origin-point))
        top-left                           (:top-left points)
        top-right                          (:top-right points)
        bottom-left                        (:bottom-left points)
        bottom-right                       (:bottom-right points)
        left                               (assoc (:left points) :y (:y origin-point))
        right                              (assoc (:right points) :y (:y origin-point))
        direction                          (direction diagonal-mode points origin-point)
        diagonal-bottom-left               (v/project-x origin-point (v/dot direction (v/v -1 1)) (:x left))
        diagonal-bottom-right              (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        angle-bottom-left                  (angle-to-point origin-point diagonal-bottom-left)
        angle-bottom-right                 (angle-to-point origin-point diagonal-bottom-right)
        line                               (-> line
                                               (update :offset max 0))
        {line-bottom-right        :line
         line-bottom-right-length :length} (line/create line
                                                        (v/abs (v/- diagonal-bottom-right origin-point))
                                                        :angle (+ angle-bottom-right 180)
                                                        :reversed? true
                                                        :render-options render-options)
        {line-bottom-left :line}           (line/create line
                                                        (v/abs (v/- diagonal-bottom-left origin-point))
                                                        :angle angle-bottom-left
                                                        :flipped? true
                                                        :render-options render-options)
        {line-top :line}                   (line/create line
                                                        (v/abs (v/- top origin-point))
                                                        :flipped? true
                                                        :angle -90
                                                        :render-options render-options)
        {line-top-reversed        :line
         line-top-reversed-length :length} (line/create line
                                                        (v/abs (v/- top origin-point))
                                                        :angle 90
                                                        :reversed? true
                                                        :render-options render-options)
        diagonal-bottom-right-adjusted     (v/extend
                                               origin-point
                                             diagonal-bottom-right
                                             line-bottom-right-length)
        top-adjusted                       (v/extend
                                               origin-point
                                             top
                                             line-top-reversed-length)
        parts                              [[["M" top-adjusted
                                              (line/stitch line-top-reversed)
                                              "L" origin-point
                                              (line/stitch line-bottom-left)
                                              (infinity/path :clockwise
                                                             [:left :top]
                                                             [diagonal-bottom-left top-adjusted])
                                              "z"]
                                             [top-left top origin-point diagonal-bottom-left]]

                                            [["M" diagonal-bottom-right-adjusted
                                              (line/stitch line-bottom-right)
                                              "L" origin-point
                                              (line/stitch line-top)
                                              (infinity/path :clockwise
                                                             [:top :right]
                                                             [top diagonal-bottom-right-adjusted])
                                              "z"]
                                             [top top-right diagonal-bottom-right origin-point]]

                                            [["M" diagonal-bottom-right-adjusted
                                              (line/stitch line-bottom-right)
                                              "L" origin-point
                                              (line/stitch line-bottom-left)
                                              (infinity/path :counter-clockwise
                                                             [:left :right]
                                                             [diagonal-bottom-left diagonal-bottom-right-adjusted])
                                              "z"]
                                             [origin-point bottom-left bottom-right]]]]
    [make-division
     (division-context-key type) fields parts
     [:all
      [(svg/make-path
        ["M" diagonal-bottom-right-adjusted
         (line/stitch line-bottom-right)])]
      nil]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline-style
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-top)])}]
        [:path {:d (svg/make-path
                    ["M" diagonal-bottom-right-adjusted
                     (line/stitch line-bottom-right)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-bottom-left)])}]])
     environment division context]))

(def divisions
  [#'per-pale
   #'per-fess
   #'per-bend
   #'per-bend-sinister
   #'per-chevron
   #'per-saltire
   #'quarterly
   #'gyronny
   #'paly
   #'barry
   #'bendy
   #'bendy-sinister
   #'tierced-per-pale
   #'tierced-per-fess
   #'tierced-per-pairle
   #'tierced-per-pairle-reversed])

(def kinds-function-map
  (->> divisions
       (map (fn [function]
              [(-> function meta :name keyword) function]))
       (into {})))

(def choices
  (->> divisions
       (map (fn [function]
              [(-> function meta :display-name) (-> function meta :name keyword)]))))

(def division-map
  (util/choices->map choices))

(defn part-name [type index]
  (let [function (get kinds-function-map type)]
    (-> function meta :parts (get index))))

(defn render [{:keys [type] :as division} environment context]
  (let [function (get kinds-function-map type)]
    [function division environment context]))
