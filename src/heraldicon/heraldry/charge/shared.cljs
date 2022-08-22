(ns heraldicon.heraldry.charge.shared
  (:require
   [clojure.string :as s]
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.line.fimbriation :as fimbriation]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.render.outline :as outline]
   [heraldicon.svg.path :as path]
   [heraldicon.svg.squiggly :as squiggly]
   [heraldicon.util.uid :as uid]))

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

(defn make-charge
  [{:keys [environment
           charge-group
           anchor-override
           size-default
           self-below-shield?
           render-pass-below-shield?
           auto-resize?]
    :or {auto-resize? true}
    :as context} arg function]
  ;; only render, if we are in the right render pass
  (if (= (boolean self-below-shield?)
         (boolean render-pass-below-shield?))
    (let [context (dissoc context
                          :anchor-override
                          :size-default
                          :charge-group)
          anchor (interface/get-sanitized-data (c/++ context :anchor))
          orientation (interface/get-sanitized-data (c/++ context :orientation))
          vertical-mask (interface/get-sanitized-data (c/++ context :vertical-mask))
          fimbriation (interface/get-sanitized-data (c/++ context :fimbriation))
          size (if (and size-default
                        (not (interface/get-raw-data (c/++ context :geometry :size))))
                 size-default
                 (interface/get-sanitized-data (c/++ context :geometry :size)))
          stretch (interface/get-sanitized-data (c/++ context :geometry :stretch))
          mirrored? (interface/get-sanitized-data (c/++ context :geometry :mirrored?))
          reversed? (interface/get-sanitized-data (c/++ context :geometry :reversed?))
          squiggly? (interface/render-option :squiggly? context)
          outline-mode (if (or (interface/render-option :outline? context)
                               (= (interface/render-option :mode context)
                                  :hatching)) :keep
                           (interface/get-sanitized-data (c/++ context :outline-mode)))
          outline? (= outline-mode :keep)
          {:keys [slot-spacing
                  slot-angle]} charge-group
          context (dissoc context :charge-group)
          environment-for-anchor (if anchor-override
                                   (assoc-in environment [:points :special] anchor-override)
                                   environment)
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
          points (:points environment)
          top (:top points)
          bottom (:bottom points)
          left (:left points)
          right (:right points)
          width (:width environment)
          height (:height environment)
          angle (+ (v/angle-to-point anchor-point orientation-point)
                   90)
          arg-value (get environment arg)

          ;; since size now is filled with a default, check whether it was set at all,
          ;; if not, then use nil; exception: if auto-resize? is false, then always use
          ;; the sanitized value
          ;; TODO: this probably needs a better mechanism and form representation
          size (when (or (not auto-resize?)
                         (interface/get-raw-data (c/++ context :geometry :size)))
                 size)
          target-arg-value (math/percent-of arg-value (or size
                                                          80))
          {:keys [shape
                  charge-width
                  charge-height
                  charge-top-left]} (function target-arg-value)
          shape (if (map? shape)
                  shape
                  {:paths [shape]})
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
                  (if (< charge-height charge-width)
                    (+ angle slot-angle 90)
                    (+ angle slot-angle))
                  angle)
          scale-x (* (if mirrored? -1 1)
                     (min (/ target-width charge-width)
                          (/ target-height charge-height)))
          scale-y (* (if reversed? -1 1)
                     (Math/abs scale-x)
                     stretch)
          charge-top-left (or charge-top-left
                              (v/div (v/Vector. charge-width charge-height)
                                     -2))
          charge-shape {:paths (into []
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
                                     (:paths shape))}
          {:keys [min-x max-x
                  min-y max-y]} (bb/rotate charge-top-left
                                           (v/add charge-top-left
                                                  (v/Vector. charge-width
                                                             charge-height))
                                           angle
                                           :middle v/zero
                                           :scale (v/Vector. scale-x scale-y))
          part [charge-shape
                [(v/add anchor-point
                        (v/Vector. min-x min-y))
                 (v/add anchor-point
                        (v/Vector. max-x max-y))]]
          charge-id (uid/generate "charge")
          vertical-mask? (not (zero? vertical-mask))
          vertical-mask-id (uid/generate "mask")]
      [:<>
       (when vertical-mask?
         (let [total-width (- max-x min-x)
               total-height (- max-y min-y)
               mask-height (math/percent-of total-height (Math/abs vertical-mask))]
           [:defs
            [:mask {:id vertical-mask-id}
             [:rect {:transform (str "translate(" (v/->str anchor-point) ")")
                     :x (- min-x 10)
                     :y (- min-y 10)
                     :width (+ total-width 20)
                     :height (+ total-height 20)
                     :style {:fill "#ffffff"}}]
             [:rect {:transform (str "translate(" (v/->str anchor-point) ")")
                     :x (- min-x 10)
                     :y (if (pos? vertical-mask)
                          (-> min-y
                              (+ total-height)
                              (- mask-height))
                          (-> min-y
                              (- 10)))
                     :width (+ total-width 20)
                     :height (+ mask-height 10)
                     :style {:fill "#000000"}}]]]))
       [:g (when vertical-mask?
             {:mask (str "url(#" vertical-mask-id ")")})
        (when (-> fimbriation :mode #{:double})
          (let [thickness (+ (math/percent-of (:thickness-1 fimbriation) charge-width)
                             (math/percent-of (:thickness-2 fimbriation) charge-width))]
            [:<>
             (when outline?
               [fimbriation/dilate-and-fill-path
                charge-shape
                nil
                (+ thickness (/ outline/stroke-width 2))
                (outline/color context) context
                :corner (:corner fimbriation)])
             [fimbriation/dilate-and-fill-path
              charge-shape
              nil
              (cond-> thickness
                outline? (- (/ outline/stroke-width 2)))
              (-> fimbriation
                  :tincture-2
                  (tincture/pick context)) context
              :corner (:corner fimbriation)]]))
        (when (-> fimbriation :mode #{:single :double})
          (let [thickness (math/percent-of (:thickness-1 fimbriation) charge-width)]
            [:<>
             (when outline?
               [fimbriation/dilate-and-fill-path
                charge-shape
                nil
                (+ thickness (/ outline/stroke-width 2))
                (outline/color context) context
                :corner (:corner fimbriation)])
             [fimbriation/dilate-and-fill-path
              charge-shape
              nil
              (cond-> thickness
                outline? (- (/ outline/stroke-width 2)))
              (-> fimbriation
                  :tincture-1
                  (tincture/pick context)) context
              :corner (:corner fimbriation)]]))
        [:g {:id charge-id}
         #_[field.shared/make-subfield
            (c/++ context :field)
            part
            :all]
         (when outline?
           [:g (outline/style context)
            [:path {:d (s/join "" (:paths charge-shape))}]])]]])
    [:<>]))

(defn process-shape [{:keys [size-default
                             anchor-override
                             charge-group
                             auto-resize?]
                      :or {auto-resize? true}
                      :as context}
                     {:keys [base-shape base-width base-height base-top-left]}]
  (let [{:keys [width height points]
         :as parent-environment} (interface/get-parent-environment context)
        {:keys [left right top bottom]} points
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
        vertical-mask (interface/get-sanitized-data (c/++ context :vertical-mask))
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
        base-top-left (or base-top-left
                          (v/div (v/Vector. base-width base-height)
                                 -2))
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
         :as bounding-box} (bb/translate
                            (bb/rotate base-top-left
                                       (v/add base-top-left
                                              (v/Vector. base-width
                                                         base-height))
                                       angle
                                       :middle v/zero
                                       :scale (v/Vector. scale-x scale-y))
                            anchor-point)
        unmasked-height (- max-y min-y)
        mask-height (math/percent-of unmasked-height (Math/abs vertical-mask))
        [min-y max-y] (if (pos? vertical-mask)
                        [min-y (max min-y (- max-y mask-height))]
                        [(min max-y (+ min-y mask-height)) max-y])
        bounding-box (assoc bounding-box
                            :min-y min-y
                            :max-y max-y)
        real-width (- max-x min-x)
        real-height (- max-y min-y)]
    {:type (interface/get-raw-data (c/++ context :type))
     :bounding-box bounding-box
     :width real-width
     :height real-height
     :shape charge-shape
     :vertical-mask vertical-mask}))

(defmethod interface/environment :heraldry/charge [context {:keys [bounding-box]}]
  (let [{:keys [meta]} (interface/get-parent-environment context)]
    (environment/create
     {:paths nil}
     (-> meta
         (dissoc :context)
         (assoc :bounding-box bounding-box)))))

(defmethod interface/render-shape :heraldry/charge [_context {:keys [shape]}]
  {:shape shape})
