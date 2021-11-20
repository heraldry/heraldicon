(ns heraldry.coat-of-arms.charge.shared
  (:require
   ["svgpath" :as svgpath]
   [clojure.string :as s]
   [heraldry.coat-of-arms.angle :as angle]
   [heraldry.coat-of-arms.field.shared :as field-shared]
   [heraldry.coat-of-arms.geometry :as geometry]
   [heraldry.coat-of-arms.line.fimbriation :as fimbriation]
   [heraldry.coat-of-arms.outline :as outline]
   [heraldry.coat-of-arms.tincture.core :as tincture]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.math.bounding-box :as bounding-box]
   [heraldry.math.svg.path :as path]
   [heraldry.math.svg.squiggly :as squiggly]
   [heraldry.math.vector :as v]
   [heraldry.options :as options]
   [heraldry.strings :as strings]
   [heraldry.util :as util]))

(defn options [context]
  (let [origin-point-option {:type :choice
                             :choices [[strings/fess-point :fess]
                                       [strings/chief-point :chief]
                                       [strings/base-point :base]
                                       [strings/dexter-point :dexter]
                                       [strings/sinister-point :sinister]
                                       [strings/honour-point :honour]
                                       [strings/nombril-point :nombril]
                                       [strings/top-left :top-left]
                                       [strings/top :top]
                                       [strings/top-right :top-right]
                                       [strings/left :left]
                                       [strings/right :right]
                                       [strings/bottom-left :bottom-left]
                                       [strings/bottom :bottom]
                                       [strings/bottom-right :bottom-right]]
                             :default :fess
                             :ui {:label strings/point}}
        anchor-point-option {:type :choice
                             :choices [[strings/top-left :top-left]
                                       [strings/top :top]
                                       [strings/top-right :top-right]
                                       [strings/left :left]
                                       [strings/right :right]
                                       [strings/bottom-left :bottom-left]
                                       [strings/bottom :bottom]
                                       [strings/bottom-right :bottom-right]
                                       [strings/fess-point :fess]
                                       [strings/chief-point :chief]
                                       [strings/base-point :base]
                                       [strings/dexter-point :dexter]
                                       [strings/sinister-point :sinister]
                                       [strings/honour-point :honour]
                                       [strings/nombril-point :nombril]
                                       [strings/angle :angle]]
                             :default :angle
                             :ui {:label strings/anchor}}
        current-anchor-point (options/get-value
                              (interface/get-raw-data (c/++ context :anchor :point))
                              anchor-point-option)]
    {:origin {:point origin-point-option
              :offset-x {:type :range
                         :min -45
                         :max 45
                         :default 0
                         :ui {:label strings/offset-x
                              :step 0.1}}
              :offset-y {:type :range
                         :min -45
                         :max 45
                         :default 0
                         :ui {:label strings/offset-y
                              :step 0.1}}
              :ui {:label strings/origin
                   :form-type :position}}
     :anchor (cond-> {:point anchor-point-option
                      :ui {:label strings/anchor
                           :form-type :position}}

               (= current-anchor-point
                  :angle) (assoc :angle {:type :range
                                         :min 0
                                         :max 360
                                         :default 0
                                         :ui {:label strings/angle}})

               (not= current-anchor-point
                     :angle) (assoc :offset-x {:type :range
                                               :min -45
                                               :max 45
                                               :default 0
                                               :ui {:label strings/offset-x
                                                    :step 0.1}}
                                    :offset-y {:type :range
                                               :min -45
                                               :max 45
                                               :default 0
                                               :ui {:label strings/offset-y
                                                    :step 0.1}}))
     :geometry geometry/default-options
     :fimbriation (-> (fimbriation/options (c/++ context :fimbriation))
                      (dissoc :alignment)
                      (options/override-if-exists [:corner :default] :round)
                      (options/override-if-exists [:thickness-1 :max :max] 50)
                      (options/override-if-exists [:thickness-1 :max :default] 10)
                      (options/override-if-exists [:thickness-2 :max :max] 50)
                      (options/override-if-exists [:thickness-2 :max :default] 10))
     :outline-mode {:type :choice
                    :choices [[{:en "Keep"
                                :de "Anzeigen"} :keep]
                              ["Transparent" :transparent]
                              [{:en "Primary"
                                :de "Primär"} :primary]
                              [{:en "Remove"
                                :de "Entfernen"} :remove]]
                    :default :keep
                    :ui {:label strings/outline}}
     :vertical-mask {:type :range
                     :default 0
                     :min -100
                     :max 100
                     :ui {:label {:en "Vertical mask"
                                  :de "Vertikale Maske"}
                          :step 1}}}))

(defn make-charge
  [{:keys [environment
           charge-group
           origin-override
           size-default
           self-below-shield?
           render-pass-below-shield?
           auto-resize?
           svg-export?]
    :or {auto-resize? true}
    :as context} arg function]
  ;; only render, if we are in the right render pass
  (if (= (boolean self-below-shield?)
         (boolean render-pass-below-shield?))
    (let [context (-> context
                      (dissoc :origin-override)
                      (dissoc :size-default)
                      (dissoc :charge-group))
          origin (interface/get-sanitized-data (c/++ context :origin))
          anchor (interface/get-sanitized-data (c/++ context :anchor))
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
          environment-for-origin (if origin-override
                                   (assoc-in environment [:points :special] origin-override)
                                   environment)
          origin (if origin-override
                   {:point :special
                    :offset-x 0
                    :offset-y 0}
                   origin)
          {origin-point :real-origin
           anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                       environment-for-origin
                                       origin
                                       anchor
                                       0
                                       -90)
          points (:points environment)
          top (:top points)
          bottom (:bottom points)
          left (:left points)
          right (:right points)
          width (:width environment)
          height (:height environment)
          angle (+ (v/angle-to-point origin-point anchor-point)
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
                             (min (- (:x origin-point) (:x left))
                                  (- (:x right) (:x origin-point))))
          min-y-distance (or (some-> slot-spacing :height (/ 2) (/ 0.8))
                             (min (- (:y origin-point) (:y top))
                                  (- (:y bottom) (:y origin-point))))
          target-width (if size
                         (-> size
                             ((util/percent-of width)))
                         (* (* min-x-distance 2) 0.8))
          target-height (/ (if size
                             (-> size
                                 ((util/percent-of height)))
                             (* (* min-y-distance 2) 0.7))
                           stretch)
          angle (if (and (-> anchor :point (= :angle))
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
                                               (->
                                                (svgpath)
                                                (.scale scale-x scale-y)
                                                (.toString))
                                               (cond->
                                                 squiggly? squiggly/squiggly-path
                                                 (not= angle 0) (->
                                                                 (svgpath)
                                                                 (.rotate angle)
                                                                 (.toString)))
                                               (path/translate (:x origin-point) (:y origin-point))))
                                     (:paths shape))}
          [min-x max-x min-y max-y] (bounding-box/rotate charge-top-left
                                                         (v/add charge-top-left
                                                                (v/v charge-width
                                                                     charge-height))
                                                         angle
                                                         :middle (v/v 0 0)
                                                         :scale (v/v scale-x scale-y))
          part [charge-shape
                [(v/add origin-point
                        (v/v min-x min-y))
                 (v/add origin-point
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
             [:rect {:transform (str "translate(" (v/->str origin-point) ")")
                     :x (- min-x 10)
                     :y (- min-y 10)
                     :width (+ total-width 20)
                     :height (+ total-height 20)
                     :style {:fill "#ffffff"}}]
             [:rect {:transform (str "translate(" (v/->str origin-point) ")")
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
                (+ thickness outline/stroke-width)
                (outline/color context) context
                :corner (-> fimbriation :corner)])
             [fimbriation/dilate-and-fill-path
              charge-shape
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
                (+ thickness outline/stroke-width)
                (outline/color context) context
                :corner (-> fimbriation :corner)])
             [fimbriation/dilate-and-fill-path
              charge-shape
              (cond-> thickness
                outline? (- outline/stroke-width))
              (-> fimbriation
                  :tincture-1
                  (tincture/pick context)) context
              :corner (-> fimbriation :corner)]]))
        [:g {:id charge-id}
         [field-shared/make-subfield
          (c/++ context :field)
          part
          :all]
         (when outline?
           [:g (outline/style context)
            [:path {:d (s/join "" (:paths charge-shape))}]])]]])
    [:<>]))
