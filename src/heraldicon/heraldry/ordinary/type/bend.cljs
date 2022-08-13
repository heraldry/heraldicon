(ns heraldicon.heraldry.ordinary.type.bend
  (:require
   [clojure.string :as s]
   [heraldicon.context :as c]
   [heraldicon.heraldry.cottising :as cottising]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.ordinary.interface :as ordinary.interface]
   [heraldicon.heraldry.ordinary.post-process :as post-process]
   [heraldicon.heraldry.ordinary.shared :as ordinary.shared]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.path :as path]))

(def ordinary-type :heraldry.ordinary.type/bend)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/bend)

(defmethod ordinary.interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside))
        anchor-point-option {:type :option.type/choice
                             :choices (position/anchor-choices
                                       [:fess
                                        :chief
                                        :base
                                        :honour
                                        :nombril
                                        :hoist
                                        :fly
                                        :top-left
                                        :bottom-right])
                             :default :top-left
                             :ui/label :string.option/point}
        current-anchor-point (options/get-value
                              (interface/get-raw-data (c/++ context :anchor :point))
                              anchor-point-option)
        orientation-point-option {:type :option.type/choice
                                  :choices (position/orientation-choices
                                            (case current-anchor-point
                                              :top-left [:fess
                                                         :chief
                                                         :base
                                                         :honour
                                                         :nombril
                                                         :bottom-right
                                                         :hoist
                                                         :fly
                                                         :center
                                                         :angle]
                                              :bottom-right [:fess
                                                             :chief
                                                             :base
                                                             :honour
                                                             :nombril
                                                             :hoist
                                                             :fly
                                                             :top-left
                                                             :center
                                                             :angle]
                                              [:top-left
                                               :bottom-right
                                               :angle]))
                                  :default (case current-anchor-point
                                             :top-left :fess
                                             :bottom-right :fess
                                             :top-left)
                                  :ui/label :string.option/point}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
    (ordinary.shared/add-humetty-and-voided
     {:anchor {:point anchor-point-option
               :alignment {:type :option.type/choice
                           :choices position/alignment-choices
                           :default :middle
                           :ui/label :string.option/alignment
                           :ui/element :ui.element/radio-select}
               :offset-x {:type :option.type/range
                          :min -75
                          :max 75
                          :default 0
                          :ui/label :string.option/offset-x
                          :ui/step 0.1}
               :offset-y {:type :option.type/range
                          :min -75
                          :max 75
                          :default 0
                          :ui/label :string.option/offset-y
                          :ui/step 0.1}
               :ui/label :string.option/anchor
               :ui/element :ui.element/position}
      :orientation (cond-> {:point orientation-point-option
                            :ui/label :string.option/orientation
                            :ui/element :ui.element/position}

                     (= current-orientation-point
                        :angle) (assoc :angle {:type :option.type/range
                                               :min 0
                                               :max 360
                                               :default 45
                                               :ui/label :string.option/angle})

                     (not= current-orientation-point
                           :angle) (assoc :alignment {:type :option.type/choice
                                                      :choices position/alignment-choices
                                                      :default :middle
                                                      :ui/label :string.option/alignment
                                                      :ui/element :ui.element/radio-select}
                                          :offset-x {:type :option.type/range
                                                     :min -75
                                                     :max 75
                                                     :default 0
                                                     :ui/label :string.option/offset-x
                                                     :ui/step 0.1}
                                          :offset-y {:type :option.type/range
                                                     :min -75
                                                     :max 75
                                                     :default 0
                                                     :ui/label :string.option/offset-y
                                                     :ui/step 0.1}))
      :line line-style
      :opposite-line opposite-line-style
      :geometry {:size {:type :option.type/range
                        :min 0.1
                        :max 90
                        :default 25
                        :ui/label :string.option/size
                        :ui/step 0.1}
                 :ui/label :string.option/geometry
                 :ui/element :ui.element/geometry}
      :outline? options/plain-outline?-option
      :cottising (cottising/add-cottising context 2)} context)))

(defmethod interface/properties ordinary-type [context]
  (let [parent-environment (interface/get-parent-environment context)
        ;; could be a bend-sinister as well
        real-ordinary-type (interface/get-raw-data (c/++ context :type))
        size (interface/get-sanitized-data (c/++ context :geometry :size))
        counterchanged? (= (interface/get-sanitized-data (c/++ context :field :type))
                           :heraldry.field.type/counterchanged)
        inherit-environment? (interface/get-sanitized-data (c/++ context :field :inherit-environment?))
        use-parent-environment? (or counterchanged?
                                    inherit-environment?)
        percentage-base (:height parent-environment)
        band-size (math/percent-of percentage-base size)
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
        {anchor-point :real-anchor
         orientation-point :real-orientation} (position/calculate-anchor-and-orientation
                                               parent-environment
                                               anchor
                                               orientation
                                               band-size
                                               nil)
        direction (v/sub orientation-point anchor-point)
        direction (-> (v/Vector. (-> direction :x Math/abs)
                                 (-> direction :y Math/abs))
                      v/normal
                      (cond->
                        (= real-ordinary-type
                           :heraldry.ordinary.type/bend-sinister) (v/dot (v/Vector. 1 -1))))
        direction-orthogonal (v/orthogonal direction)
        direction-orthogonal (if (neg? (:y direction-orthogonal))
                               (v/mul direction-orthogonal -1)
                               direction-orthogonal)
        parent-shape (interface/get-exact-parent-shape context)
        [middle-start middle-end] (v/intersections-with-shape anchor-point (v/add anchor-point direction)
                                                              parent-shape :default? true)
        angle (v/angle-to-point middle-start middle-end)
        width-offset (-> direction-orthogonal
                         (v/mul band-size)
                         (v/div 2))
        upper-start (v/sub middle-start width-offset)
        upper-end (v/sub middle-end width-offset)
        lower-start (v/add middle-start width-offset)
        lower-end (v/add middle-end width-offset)
        [upper-left upper-right] (v/intersections-with-shape upper-start upper-end parent-shape :default? true)
        [lower-left lower-right] (v/intersections-with-shape lower-start lower-end parent-shape :default? true)
        line-length (max (v/abs (v/sub upper-left upper-right))
                         (v/abs (v/sub middle-start middle-end))
                         (v/abs (v/sub lower-left lower-right))
                         10)
        ordinary-top-left (first (sort-by
                                  (fn [v]
                                    (let [rv (:x (v/rotate v (- angle)))]
                                      (if (math/close-to-zero? rv)
                                        0
                                        rv)))
                                  [upper-start
                                   lower-start
                                   upper-left]))
        reverse-transform-fn (when-not use-parent-environment?
                               (fn reverse-transform-fn [v]
                                 (if (instance? v/Vector v)
                                   (-> v
                                       (v/sub ordinary-top-left)
                                       (v/rotate (- angle)))
                                   (-> v
                                       (path/translate (- (:x ordinary-top-left)) (- (:y ordinary-top-left)))
                                       (path/rotate (- angle))))))
        lower-left (v/add upper-left (v/mul width-offset 2))]
    (post-process/properties
     {:type ordinary-type
      :upper [upper-left upper-right]
      :lower [lower-left lower-right]
      :angle angle
      :direction-orthogonal direction-orthogonal
      :band-size band-size
      :line-length line-length
      :percentage-base percentage-base
      :use-parent-environment? use-parent-environment?
      :transform (when-not use-parent-environment?
                   (str "translate(" (v/->str ordinary-top-left) ")"
                        "rotate(" angle ")"))
      :reverse-transform-fn reverse-transform-fn
      :humetty-percentage-base (min (:width parent-environment)
                                    (:height parent-environment))
      :voided-percentage-base band-size}
     context)))

(defmethod interface/environment ordinary-type [context {:keys [reverse-transform-fn]
                                                         [upper-left upper-right] :upper
                                                         [lower-left lower-right] :lower}]
  (let [{:keys [meta]} (interface/get-parent-environment context)
        ;; TODO: this should be more accurate
        bounding-box-points (cond->> [upper-left upper-right
                                      lower-left lower-right]
                              reverse-transform-fn (map reverse-transform-fn))]
    (environment/create
     {:paths nil}
     (-> meta
         (dissoc :context)
         (merge {:bounding-box (bb/from-points bounding-box-points)})))))

(defmethod interface/render-shape ordinary-type [context {:keys [line opposite-line]
                                                          [upper-left upper-right] :upper
                                                          [lower-left lower-right] :lower
                                                          :as properties}]
  (let [{:keys [meta]} (interface/get-parent-environment context)
        bounding-box (:bounding-box meta)
        {line-upper :line
         line-upper-start :line-start
         line-upper-from :adjusted-from
         :as line-upper-data} (line/create-with-extension line
                                                          upper-left upper-right
                                                          bounding-box
                                                          :context context)
        {line-lower :line
         line-lower-start :line-start
         line-lower-to :adjusted-to
         :as line-lower-data} (line/create-with-extension opposite-line
                                                          lower-left lower-right
                                                          bounding-box
                                                          :reversed? true
                                                          :context context)]
    (post-process/shape
     {:shape [(path/make-path
               ["M" (v/add line-upper-from line-upper-start)
                (path/stitch line-upper)
                "L" (v/add line-lower-to line-lower-start)
                (path/stitch line-lower)
                "z"])]
      :lines [{:line line
               :line-from line-upper-from
               :line-data [line-upper-data]}
              {:line opposite-line
               :line-from line-lower-to
               :line-data [line-lower-data]}]}
     context
     properties)))

(defmethod interface/exact-shape ordinary-type [context {:keys [reverse-transform-fn]}]
  (let [exact-shape (interface/fallback-exact-shape context)]
    (if reverse-transform-fn
      (-> exact-shape
          path/parse-path
          reverse-transform-fn
          path/to-svg)
      exact-shape)))

(defmethod cottising/cottise-properties ordinary-type [context
                                                       {:keys [line-length percentage-base direction-orthogonal
                                                               angle flip-cottise? humetty]
                                                        real-ordinary-type :type
                                                        [reference-upper-left reference-upper-right] :upper
                                                        [reference-lower-left reference-lower-right] :lower
                                                        reference-upper-line :line
                                                        reference-lower-line :opposite-line}]
  (let [kind (cottising/kind context)
        distance (interface/get-sanitized-data (c/++ context :distance))
        distance (math/percent-of percentage-base distance)
        thickness (interface/get-sanitized-data (c/++ context :thickness))
        band-size (math/percent-of percentage-base thickness)
        counterchanged? (= (interface/get-sanitized-data (c/++ context :field :type))
                           :heraldry.field.type/counterchanged)
        inherit-environment? (interface/get-sanitized-data (c/++ context :field :inherit-environment?))
        use-parent-environment? (or counterchanged?
                                    inherit-environment?)
        line (line/resolve-percentages (interface/get-sanitized-data (c/++ context :line))
                                       line-length percentage-base)
        opposite-line (line/resolve-percentages (interface/get-sanitized-data (c/++ context :opposite-line))
                                                line-length percentage-base)
        opposite? (or flip-cottise?
                      (-> kind name (s/starts-with? "cottise-opposite")))
        reference-line (if opposite?
                         reference-lower-line
                         reference-upper-line)
        real-distance (+ (:effective-height reference-line)
                         distance)
        [base-left base-right] (if opposite?
                                 [reference-lower-left reference-lower-right]
                                 [reference-upper-left reference-upper-right])
        dist-vector (v/mul direction-orthogonal real-distance)
        band-size-vector (v/mul direction-orthogonal band-size)
        add-fn (if opposite?
                 v/add
                 v/sub)
        [first-left first-right] (map #(add-fn % dist-vector) [base-left base-right])
        [second-left second-right] (map #(add-fn % band-size-vector) [first-left first-right])
        [upper-left upper-right
         lower-left lower-right] (if opposite?
                                   [first-left first-right
                                    second-left second-right]
                                   [second-left second-right
                                    first-left first-right])
        reverse-transform-fn (when-not use-parent-environment?
                               (fn reverse-transform-fn [v]
                                 (if (instance? v/Vector v)
                                   (-> v
                                       (v/sub upper-left)
                                       (v/rotate (- angle)))
                                   (-> v
                                       (path/translate (- (:x upper-left)) (- (:y upper-left)))
                                       (path/rotate (- angle))))))
        [line opposite-line] (if opposite?
                               [opposite-line line]
                               [line opposite-line])]
    (post-process/properties
     {:type real-ordinary-type
      :upper [upper-left upper-right]
      :lower [lower-left lower-right]
      :angle angle
      :direction-orthogonal direction-orthogonal
      :band-size band-size
      :line-length line-length
      :percentage-base percentage-base
      :use-parent-environment? false
      :transform (when-not use-parent-environment?
                   (str "translate(" (v/->str upper-left) ")"
                        "rotate(" angle ")"))
      :reverse-transform-fn reverse-transform-fn
      :line line
      :opposite-line opposite-line
      :humetty humetty}
     context)))
