(ns heraldicon.heraldry.ordinary.type.gore
  (:require
   [clojure.string :as str]
   [heraldicon.context :as c]
   [heraldicon.heraldry.cottising :as cottising]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.ordinary.interface :as ordinary.interface]
   [heraldicon.heraldry.ordinary.post-process :as post-process]
   [heraldicon.heraldry.ordinary.shared :as ordinary.shared]
   [heraldicon.interface :as interface]
   [heraldicon.math.angle :as angle]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.shape :as shape]))

(def ordinary-type :heraldry.ordinary.type/gore)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/gore)

(defmethod ordinary.interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil)
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil)
                                (dissoc :fimbriation))
        orientation-point-option {:type :option.type/choice
                                  :choices (position/orientation-choices
                                            [:top-left
                                             :top-right
                                             :angle])
                                  :default :top-left
                                  :ui/label :string.option/point}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
    (ordinary.shared/add-humetty-and-voided
     {:adapt-to-ordinaries? {:type :option.type/boolean
                             :default true
                             :ui/label :string.option/adapt-to-ordinaries?}
      :anchor {:point {:type :option.type/choice
                       :choices (position/anchor-choices
                                 [:fess
                                  :chief
                                  :base
                                  :hoist
                                  :fly
                                  :center])
                       :default :fess
                       :ui/label :string.option/point}
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
                                               :min -80
                                               :max 80
                                               :default -45
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
      :line line-style
      :opposite-line opposite-line-style
      :outline? options/plain-outline?-option
      :cottising (cottising/add-cottising context 1)} context)))

(defmethod interface/properties ordinary-type [context]
  (let [{:keys [width]
         :as parent-environment} (interface/get-parent-field-environment context)
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
        percentage-base width
        parent-shape (interface/get-parent-field-shape context)
        {anchor-point :real-anchor
         orientation-point :real-orientation} (position/calculate-anchor-and-orientation
                                               parent-environment
                                               anchor
                                               orientation
                                               0
                                               -90)
        intersection-left (v/last-intersection-with-shape anchor-point orientation-point parent-shape :default? true)
        intersection-right (v/last-intersection-with-shape anchor-point (v/add anchor-point (v/Vector. 0 50)) parent-shape :default? true)
        sinister? (> (:x intersection-left)
                     (:x anchor-point))
        [intersection-left intersection-right] (if sinister?
                                                 [intersection-right intersection-left]
                                                 [intersection-left intersection-right])
        line-length (max (v/abs (v/sub intersection-left anchor.point))
                         (v/abs (v/sub intersection-right anchor-point)))]
    (post-process/properties
     {:type ordinary-type
      :edge [intersection-left anchor-point intersection-right]
      :sinister? sinister?
      :line-length line-length
      :percentage-base percentage-base
      :humetty-percentage-base width
      :voided-percentage-base (/ width 2)}
     context)))

(defmethod interface/environment ordinary-type [context]
  (let [{:keys [sinister?]
         [left point right] :edge} (interface/get-properties context)
        {:keys [points]} (interface/get-parent-field-environment context)
        side-point (if sinister?
                     (:right points)
                     (:left points))
        ;; TODO: needs to be improved
        bounding-box-points [point left right side-point]]
    (environment/create (bb/from-points bounding-box-points))))

(defmethod interface/render-shape ordinary-type [context]
  (let [{:keys [line opposite-line]
         [left point right] :edge
         :as properties} (interface/get-properties context)
        {:keys [bounding-box]} (interface/get-parent-field-environment context)
        line-left (line/create-with-extension context
                                              line
                                              point left
                                              bounding-box
                                              :reversed? true
                                              :extend-from? false)
        line-right (line/create-with-extension context
                                               opposite-line
                                               point right
                                               bounding-box
                                               :extend-from? false)]
    (post-process/shape
     {:shape [(shape/build-shape
               context
               line-left
               line-right
               :clockwise)]
      :edges [{:lines [line-left line-right]}]}
     context
     properties)))

(defmethod cottising/cottise-properties ordinary-type [context
                                                       {:keys [line-length percentage-base humetty]
                                                        [reference-first reference-point reference-second] :edge
                                                        sinister? :sinister?
                                                        reference-line :line}]
  (when-not (-> (cottising/kind context) name (str/starts-with? "cottise-opposite"))
    (let [distance (interface/get-sanitized-data (c/++ context :distance))
          distance (math/percent-of percentage-base distance)
          thickness (interface/get-sanitized-data (c/++ context :thickness))
          band-size (math/percent-of percentage-base thickness)
          [base-corner base-left base-right] [reference-point reference-first reference-second]
          joint-angle (if sinister?
                        (angle/normalize (- 90 (v/angle-to-point base-corner base-right)))
                        (angle/normalize (- (v/angle-to-point base-corner base-left) 90)))
          real-distance (/ (+ (:effective-height reference-line)
                              distance)
                           (Math/sin (-> joint-angle
                                         (* Math/PI)
                                         (/ 180)
                                         (/ 2))))
          gore-angle (if sinister?
                       (- 90 (/ joint-angle 2))
                       (+ 90 (/ joint-angle 2)))
          delta (/ band-size
                   (Math/sin (-> joint-angle
                                 (* Math/PI)
                                 (/ 180)
                                 (/ 2))))
          dist-vector (v/rotate
                       (v/Vector. real-distance 0)
                       gore-angle)
          band-size-vector (v/rotate
                            (v/Vector. delta 0)
                            gore-angle)
          lower-corner (v/sub base-corner dist-vector)
          upper-corner (v/sub lower-corner band-size-vector)
          [lower-left lower-right] (map #(v/sub % dist-vector) [base-left base-right])
          [upper-left upper-right] (map #(v/sub % band-size-vector) [lower-left lower-right])]
      (post-process/properties
       {:type :heraldry.ordinary.type/chevron
        :upper [upper-left upper-corner upper-right]
        :lower [lower-left lower-corner lower-right]
        :chevron-angle gore-angle
        :joint-angle joint-angle
        :band-size band-size
        :line-length line-length
        :percentage-base percentage-base
        :humetty humetty}
       context))))
