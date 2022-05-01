(ns heraldicon.heraldry.charge.shared
  (:require
   [clojure.string :as s]
   [heraldicon.heraldry.field.shared :as field.shared]
   [heraldicon.heraldry.line.fimbriation :as fimbriation]
   [heraldicon.heraldry.orientation :as orientation]
   [heraldicon.heraldry.outline :as outline]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bounding-box]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.path :as path]
   [heraldicon.svg.squiggly :as squiggly]
   [heraldicon.util :as util]))

(defn options [context]
  (let [anchor-point-option {:type :choice
                             :choices [[:string.option.point-choice/fess :fess]
                                       [:string.option.point-choice/chief :chief]
                                       [:string.option.point-choice/base :base]
                                       [:string.option.point-choice/dexter :dexter]
                                       [:string.option.point-choice/sinister :sinister]
                                       [:string.option.point-choice/honour :honour]
                                       [:string.option.point-choice/nombril :nombril]
                                       [:string.option.point-choice/top-left :top-left]
                                       [:string.option.point-choice/top :top]
                                       [:string.option.point-choice/top-right :top-right]
                                       [:string.option.point-choice/left :left]
                                       [:string.option.point-choice/right :right]
                                       [:string.option.point-choice/bottom-left :bottom-left]
                                       [:string.option.point-choice/bottom :bottom]
                                       [:string.option.point-choice/bottom-right :bottom-right]]
                             :default :fess
                             :ui {:label :string.option/point}}
        orientation-point-option {:type :choice
                                  :choices [[:string.option.point-choice/top-left :top-left]
                                            [:string.option.point-choice/top :top]
                                            [:string.option.point-choice/top-right :top-right]
                                            [:string.option.point-choice/left :left]
                                            [:string.option.point-choice/right :right]
                                            [:string.option.point-choice/bottom-left :bottom-left]
                                            [:string.option.point-choice/bottom :bottom]
                                            [:string.option.point-choice/bottom-right :bottom-right]
                                            [:string.option.point-choice/fess :fess]
                                            [:string.option.point-choice/chief :chief]
                                            [:string.option.point-choice/base :base]
                                            [:string.option.point-choice/dexter :dexter]
                                            [:string.option.point-choice/sinister :sinister]
                                            [:string.option.point-choice/honour :honour]
                                            [:string.option.point-choice/nombril :nombril]
                                            [:string.option.orientation-point-choice/angle :angle]]
                                  :default :angle
                                  :ui {:label :string.option/orientation}}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
    {:anchor {:point anchor-point-option
              :offset-x {:type :range
                         :min -45
                         :max 45
                         :default 0
                         :ui {:label :string.option/offset-x
                              :step 0.1}}
              :offset-y {:type :range
                         :min -45
                         :max 45
                         :default 0
                         :ui {:label :string.option/offset-y
                              :step 0.1}}
              :ui {:label :string.option/anchor
                   :form-type :position}}
     :orientation (cond-> {:point orientation-point-option
                           :ui {:label :string.option/orientation
                                :form-type :position}}

                    (= current-orientation-point
                       :angle) (assoc :angle {:type :range
                                              :min 0
                                              :max 360
                                              :default 0
                                              :ui {:label :string.option/angle}})

                    (not= current-orientation-point
                          :angle) (assoc :offset-x {:type :range
                                                    :min -45
                                                    :max 45
                                                    :default 0
                                                    :ui {:label :string.option/offset-x
                                                         :step 0.1}}
                                         :offset-y {:type :range
                                                    :min -45
                                                    :max 45
                                                    :default 0
                                                    :ui {:label :string.option/offset-y
                                                         :step 0.1}}))
     :geometry {:size {:type :range
                       :min 5
                       :max 250
                       :default 50
                       :ui {:label :string.option/size
                            :step 0.1}}
                :stretch {:type :range
                          :min 0.33
                          :max 3
                          :default 1
                          :ui {:label :string.option/stretch
                               :step 0.01}}
                :mirrored? {:type :boolean
                            :default false
                            :ui {:label :string.option/mirrored?}}
                :reversed? {:type :boolean
                            :default false
                            :ui {:label :string.option/reversed?}}
                :ui {:label :string.option/geometry
                     :form-type :geometry}}
     :fimbriation (-> (fimbriation/options (c/++ context :fimbriation))
                      (dissoc :alignment)
                      (options/override-if-exists [:corner :default] :round)
                      (options/override-if-exists [:thickness-1 :max :max] 50)
                      (options/override-if-exists [:thickness-1 :max :default] 10)
                      (options/override-if-exists [:thickness-2 :max :max] 50)
                      (options/override-if-exists [:thickness-2 :max :default] 10))
     :outline-mode {:type :choice
                    :choices [[:string.option.outline-mode-choice/keep :keep]
                              [:string.option.outline-mode-choice/transparent :transparent]
                              [:string.option.outline-mode-choice/primary :primary]
                              [:string.option.outline-mode-choice/remove :remove]]
                    :default :keep
                    :ui {:label :string.option/outline-mode}}
     :vertical-mask {:type :range
                     :default 0
                     :min -100
                     :max 100
                     :ui {:label :string.option/vertical-mask
                          :step 1}}}))

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
    (let [context (-> context
                      (dissoc :anchor-override)
                      (dissoc :size-default)
                      (dissoc :charge-group))
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
           orientation-point :real-orientation} (orientation/calculate-anchor-and-orientation
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
          size (if (or (not auto-resize?)
                       (interface/get-raw-data (c/++ context :geometry :size)))
                 size
                 nil)
          target-arg-value (-> (or size
                                   80)
                               ((util/percent-of arg-value)))
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
                         (-> size
                             ((util/percent-of width)))
                         (* (* min-x-distance 2) 0.8))
          target-height (/ (if size
                             (-> size
                                 ((util/percent-of height)))
                             (* (* min-y-distance 2) 0.7))
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
                     (* (Math/abs scale-x) stretch))
          charge-top-left (or charge-top-left
                              (-> (v/v charge-width charge-height)
                                  (v/div -2)))
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
          [min-x max-x min-y max-y] (bounding-box/rotate charge-top-left
                                                         (v/add charge-top-left
                                                                (v/v charge-width
                                                                     charge-height))
                                                         angle
                                                         :middle (v/v 0 0)
                                                         :scale (v/v scale-x scale-y))
          part [charge-shape
                [(v/add anchor-point
                        (v/v min-x min-y))
                 (v/add anchor-point
                        (v/v max-x max-y))]]
          charge-id (util/id "charge")
          vertical-mask? (not (zero? vertical-mask))
          vertical-mask-id (util/id "mask")]
      [:<>
       (when vertical-mask?
         (let [total-width (- max-x min-x)
               total-height (- max-y min-y)
               mask-height ((util/percent-of total-height) (Math/abs vertical-mask))]
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
          (let [thickness (+ (-> fimbriation
                                 :thickness-1
                                 ((util/percent-of charge-width)))
                             (-> fimbriation
                                 :thickness-2
                                 ((util/percent-of charge-width))))]
            [:<>
             (when outline?
               [fimbriation/dilate-and-fill-path
                charge-shape
                nil
                (+ thickness outline/stroke-width)
                (outline/color context) context
                :corner (-> fimbriation :corner)])
             [fimbriation/dilate-and-fill-path
              charge-shape
              nil
              (cond-> thickness
                outline? (- outline/stroke-width))
              (-> fimbriation
                  :tincture-2
                  (tincture/pick context)) context
              :corner (-> fimbriation :corner)]]))
        (when (-> fimbriation :mode #{:single :double})
          (let [thickness (-> fimbriation
                              :thickness-1
                              ((util/percent-of charge-width)))]
            [:<>
             (when outline?
               [fimbriation/dilate-and-fill-path
                charge-shape
                nil
                (+ thickness outline/stroke-width)
                (outline/color context) context
                :corner (-> fimbriation :corner)])
             [fimbriation/dilate-and-fill-path
              charge-shape
              nil
              (cond-> thickness
                outline? (- outline/stroke-width))
              (-> fimbriation
                  :tincture-1
                  (tincture/pick context)) context
              :corner (-> fimbriation :corner)]]))
        [:g {:id charge-id}
         [field.shared/make-subfield
          (c/++ context :field)
          part
          :all]
         (when outline?
           [:g (outline/style context)
            [:path {:d (s/join "" (:paths charge-shape))}]])]]])
    [:<>]))
