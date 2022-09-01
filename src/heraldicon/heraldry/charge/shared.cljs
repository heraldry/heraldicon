(ns heraldicon.heraldry.charge.shared
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.line.fimbriation :as fimbriation]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.path :as path]
   [heraldicon.svg.squiggly :as squiggly]))

(def ^:private outline-mode-choices
  [[:string.option.outline-mode-choice/keep :keep]
   [:string.option.outline-mode-choice/transparent :transparent]
   [:string.option.outline-mode-choice/primary :primary]
   [:string.option.outline-mode-choice/remove :remove]])

(def outline-mode-map
  (options/choices->map outline-mode-choices))

(defn options [context]
  (let [anchor-point-option {:type :option.type/choice
                             :choices (position/anchor-choices
                                       [:fess
                                        :chief
                                        :base
                                        :dexter
                                        :sinister
                                        :honour
                                        :nombril
                                        :hoist
                                        :fly
                                        :top-left
                                        :top
                                        :top-right
                                        :left
                                        :center
                                        :right
                                        :bottom-left
                                        :bottom
                                        :bottom-right])
                             :default :fess
                             :ui/label :string.option/point}
        orientation-point-option {:type :option.type/choice
                                  :choices (position/orientation-choices
                                            [:top-left
                                             :top
                                             :top-right
                                             :left
                                             :center
                                             :right
                                             :bottom-left
                                             :bottom
                                             :bottom-right
                                             :fess
                                             :chief
                                             :base
                                             :dexter
                                             :sinister
                                             :honour
                                             :nombril
                                             :angle])
                                  :default :angle
                                  :ui/label :string.option/orientation}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
    {:anchor {:point anchor-point-option
              :offset-x {:type :option.type/range
                         :min -45
                         :max 45
                         :default 0
                         :ui/label :string.option/offset-x
                         :ui/step 0.1}
              :offset-y {:type :option.type/range
                         :min -45
                         :max 45
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
                                              :default 0
                                              :ui/label :string.option/angle})

                    (not= current-orientation-point
                          :angle) (assoc :offset-x {:type :option.type/range
                                                    :min -45
                                                    :max 45
                                                    :default 0
                                                    :ui/label :string.option/offset-x
                                                    :ui/step 0.1}
                                         :offset-y {:type :option.type/range
                                                    :min -45
                                                    :max 45
                                                    :default 0
                                                    :ui/label :string.option/offset-y
                                                    :ui/step 0.1}))
     :geometry {:size {:type :option.type/range
                       :min 5
                       :max 250
                       :default 50
                       :ui/label :string.option/size
                       :ui/step 0.1}
                :stretch {:type :option.type/range
                          :min 0.33
                          :max 3
                          :default 1
                          :ui/label :string.option/stretch
                          :ui/step 0.01}
                :mirrored? {:type :option.type/boolean
                            :default false
                            :ui/label :string.option/mirrored?}
                :reversed? {:type :option.type/boolean
                            :default false
                            :ui/label :string.option/reversed?}
                :ui/label :string.option/geometry
                :ui/element :ui.element/geometry}
     :fimbriation (-> (fimbriation/options (c/++ context :fimbriation))
                      (dissoc :alignment)
                      (options/override-if-exists [:corner :default] :round)
                      (options/override-if-exists [:thickness-1 :max] 25)
                      (options/override-if-exists [:thickness-1 :default] 2.5)
                      (options/override-if-exists [:thickness-2 :max] 25)
                      (options/override-if-exists [:thickness-2 :default] 1.5))
     :outline-mode {:type :option.type/choice
                    :choices outline-mode-choices
                    :default :keep
                    :ui/label :string.option/outline-mode}
     :vertical-mask {:type :option.type/range
                     :default 0
                     :min -100
                     :max 100
                     :ui/label :string.option/vertical-mask
                     :ui/step 1}}))

(defn apply-vertical-mask [context {:keys [bounding-box width height]
                                    :as properties}]
  (let [vertical-mask (interface/get-sanitized-data (c/++ context :vertical-mask))
        {:keys [min-x max-x
                min-y max-y]} bounding-box
        unmasked-height (- max-y min-y)
        mask-height (math/percent-of unmasked-height (Math/abs vertical-mask))
        [min-y max-y] (if (pos? vertical-mask)
                        [min-y (max min-y (- max-y mask-height))]
                        [(min max-y (+ min-y mask-height)) max-y])
        bounding-box (assoc bounding-box
                            :min-y min-y
                            :max-y max-y)
        [real-width real-height] (bb/size bounding-box)
        vertical-mask-shape (when-not (zero? vertical-mask)
                              (let [fimbriation-percentage-base (min width height)
                                    {:keys [mode
                                            thickness-1
                                            thickness-2]} (some-> (interface/get-sanitized-data (c/++ context :fimbriation))
                                                                  (update :thickness-1 (partial math/percent-of fimbriation-percentage-base))
                                                                  (update :thickness-2 (partial math/percent-of fimbriation-percentage-base)))
                                    margin (+ 3
                                              (case mode
                                                :double (+ thickness-1 thickness-2)
                                                :single thickness-1
                                                0))
                                    [mask-min-x mask-max-x] [(- min-x margin) (+ max-x margin)]
                                    [mask-min-y mask-max-y] (if (pos? vertical-mask)
                                                              [(- min-y margin) max-y]
                                                              [min-y (+ max-y margin)])]
                                (path/make-path
                                 ["M" mask-min-x mask-min-y
                                  "H" mask-max-x
                                  "V" mask-max-y
                                  "H" mask-min-x
                                  "z"])))]
    (assoc properties
           :bounding-box bounding-box
           :width real-width
           :height real-height
           :vertical-mask-shape vertical-mask-shape)))

(defn process-shape [{:keys [size-default
                             anchor-override
                             charge-group
                             auto-resize?]
                      :or {auto-resize? true}
                      :as context}
                     {:keys [base-shape base-width base-height]
                      :as base-properties}]
  (let [{:keys [width height points]
         :as parent-environment} (interface/get-parent-environment context)
        {:keys [left right top bottom]} points
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
        size (if (interface/get-raw-data (c/++ context :geometry :size))
               (interface/get-sanitized-data (c/++ context :geometry :size))
               (if auto-resize?
                 size-default
                 nil))
        stretch (interface/get-sanitized-data (c/++ context :geometry :stretch))
        mirrored? (interface/get-sanitized-data (c/++ context :geometry :mirrored?))
        reversed? (interface/get-sanitized-data (c/++ context :geometry :reversed?))
        squiggly? (interface/render-option :squiggly? context)
        {:keys [slot-spacing
                slot-angle]} charge-group
        environment-for-anchor (if anchor-override
                                 (assoc-in parent-environment [:points :special] anchor-override)
                                 parent-environment)
        anchor (if anchor-override
                 {:point :special
                  :offset-x 0
                  :offset-y 0}
                 anchor)
        {anchor-point :real-anchor
         orientation-point :real-orientation} (position/calculate-anchor-and-orientation
                                               environment-for-anchor
                                               anchor
                                               orientation
                                               0
                                               -90)
        angle (+ (v/angle-to-point anchor-point orientation-point)
                 90)
        min-x-distance (or (some-> slot-spacing :width (/ 2) (/ 0.9))
                           (min (- (:x anchor-point) (:x left))
                                (- (:x right) (:x anchor-point))))
        min-y-distance (or (some-> slot-spacing :height (/ 2) (/ 0.8))
                           (min (- (:y anchor-point) (:y top))
                                (- (:y bottom) (:y anchor-point))))
        target-width (if size
                       (math/percent-of width size)
                       (* min-x-distance 2 0.8))
        target-height (/ (if size
                           (math/percent-of height size)
                           (* min-y-distance 2 0.7))
                         stretch)
        angle (if (and (-> orientation :point (= :angle))
                       slot-angle)
                (if (< base-height base-width)
                  (+ angle slot-angle 90)
                  (+ angle slot-angle))
                angle)
        scale-x (* (if mirrored? -1 1)
                   (min (/ target-width base-width)
                        (/ target-height base-height)))
        scale-y (* (if reversed? -1 1)
                   (Math/abs scale-x)
                   stretch)
        base-top-left (v/div (v/Vector. base-width base-height)
                             -2)
        charge-shape (into []
                           (map #(-> %
                                     path/make-path
                                     path/parse-path
                                     (path/scale scale-x scale-y)
                                     (cond->
                                       squiggly? (->
                                                   path/to-svg
                                                   squiggly/squiggly-path
                                                   path/parse-path)
                                       (not= angle 0) (.rotate angle))
                                     (path/translate (:x anchor-point) (:y anchor-point))
                                     path/to-svg))
                           base-shape)
        {:keys [min-x max-x
                min-y max-y]
         :as bounding-box} (-> (bb/from-vector-and-size
                                base-top-left base-width base-height)
                               (bb/rotate angle
                                          :middle v/zero
                                          :scale (v/Vector. scale-x scale-y))
                               (bb/translate anchor-point))]
    (apply-vertical-mask
     context
     (merge base-properties
            {:type (interface/get-raw-data (c/++ context :type))
             :bounding-box bounding-box
             :width (- max-x min-x)
             :height (- max-y min-y)
             :shape charge-shape
             :anchor-point anchor-point
             :scale-x scale-x
             :scale-y scale-y
             :angle angle
             :top-left base-top-left}))))

(defmethod interface/environment :heraldry/charge [_context {:keys [bounding-box anchor-point]}]
  (environment/create bounding-box {:fess anchor-point}))

(defmethod interface/bounding-box :heraldry/charge [_context {:keys [bounding-box]}]
  bounding-box)

(defmethod interface/render-shape :heraldry/charge [_context {:keys [shape]}]
  {:shape shape})
