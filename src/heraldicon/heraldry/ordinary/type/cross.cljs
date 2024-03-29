(ns heraldicon.heraldry.ordinary.type.cross
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
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.shape :as shape]))

(def ordinary-type :heraldry.ordinary.type/cross)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/cross)

(defmethod ordinary.interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil)
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside))]
    (ordinary.shared/add-humetty-and-voided
     {:adapt-to-ordinaries? {:type :option.type/boolean
                             :default true
                             :ui/label :string.option/adapt-to-ordinaries?}
      :anchor {:point {:type :option.type/choice
                       :choices (position/anchor-choices
                                 [:chief
                                  :base
                                  :fess
                                  :dexter
                                  :sinister
                                  :hoist
                                  :fly
                                  :honour
                                  :nombril
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
      :cottising (cottising/add-cottising context 1)} context)))

(defmethod interface/properties ordinary-type [context]
  (let [{:keys [width height]
         :as parent-environment} (interface/get-parent-field-environment context)
        size (interface/get-sanitized-data (c/++ context :geometry :size))
        percentage-base (min width height)
        band-size (math/percent-of percentage-base size)
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        anchor-point (position/calculate anchor parent-environment :fess)
        parent-shape (interface/get-parent-field-shape context)
        upper (- (:y anchor-point) (/ band-size 2))
        lower (+ upper band-size)
        first (- (:x anchor-point) (/ band-size 2))
        second (+ first band-size)
        corner-top-left (v/Vector. first upper)
        corner-top-right (v/Vector. second upper)
        corner-bottom-left (v/Vector. first lower)
        corner-bottom-right (v/Vector. second lower)
        left-1 (v/last-intersection-with-shape corner-top-left (v/Vector. -30 0)
                                               parent-shape :default? true :relative? true)
        right-1 (v/last-intersection-with-shape corner-top-right (v/Vector. 30 0)
                                                parent-shape :default? true :relative? true)
        left-2 (v/last-intersection-with-shape corner-bottom-left (v/Vector. -30 0)
                                               parent-shape :default? true :relative? true)
        right-2 (v/last-intersection-with-shape corner-bottom-right (v/Vector. 30 0)
                                                parent-shape :default? true :relative? true)
        top-1 (v/last-intersection-with-shape corner-top-left (v/Vector. 0 -30)
                                              parent-shape :default? true :relative? true)
        bottom-1 (v/last-intersection-with-shape corner-bottom-left (v/Vector. 0 30)
                                                 parent-shape :default? true :relative? true)
        top-2 (v/last-intersection-with-shape corner-top-right (v/Vector. 0 -30)
                                              parent-shape :default? true :relative? true)
        bottom-2 (v/last-intersection-with-shape corner-bottom-right (v/Vector. 0 30)
                                                 parent-shape :default? true :relative? true)
        line-length (->> (concat (map (fn [v]
                                        (v/sub v corner-top-left))
                                      [top-1 left-1])
                                 (map (fn [v]
                                        (v/sub v corner-top-right))
                                      [top-2 right-1])
                                 (map (fn [v]
                                        (v/sub v corner-bottom-left))
                                      [bottom-1 left-2])
                                 (map (fn [v]
                                        (v/sub v corner-bottom-right))
                                      [bottom-2 right-2]))
                         (map v/abs)
                         (apply max))]
    (post-process/properties
     {:type ordinary-type
      :edge-top-left [left-1 corner-top-left top-1]
      :edge-top-right [top-2 corner-top-right right-1]
      :edge-bottom-left [bottom-1 corner-bottom-left left-2]
      :edge-bottom-right [right-2 corner-bottom-right bottom-2]
      :band-size band-size
      :line-length line-length
      :percentage-base percentage-base
      :humetty-percentage-base percentage-base
      :voided-percentage-base band-size
      :num-cottise-parts 4}
     context)))

(defmethod interface/environment ordinary-type [context]
  (let [{[left-1 corner-top-left top-1] :edge-top-left
         [top-2 corner-top-right right-1] :edge-top-right
         [bottom-1 corner-bottom-left left-2] :edge-bottom-left
         [right-2 corner-bottom-right bottom-2] :edge-bottom-right} (interface/get-properties context)
        {:keys [points]} (interface/get-parent-field-environment context)
        bounding-box-points [top-1 top-2
                             bottom-1 bottom-2
                             left-1 left-2
                             right-1 right-2
                             (:bottom points)
                             (:top points)
                             (:left points)
                             (:right points)]]
    ;; TODO: maybe best to inherit the parent environment, unless the cross is couped
    (environment/create (bb/from-points bounding-box-points)
                        {:fess (v/avg corner-top-left
                                      corner-top-right
                                      corner-bottom-left
                                      corner-bottom-right)})))

(defmethod interface/render-shape ordinary-type [context]
  (let [{:keys [line opposite-line]
         [left-1 corner-top-left top-1] :edge-top-left
         [top-2 corner-top-right right-1] :edge-top-right
         [bottom-1 corner-bottom-left left-2] :edge-bottom-left
         [right-2 corner-bottom-right bottom-2] :edge-bottom-right
         :as properties} (interface/get-properties context)
        {:keys [bounding-box]} (interface/get-parent-field-environment context)
        line-edge-top-left-first (line/create-with-extension context
                                                             line
                                                             corner-top-left left-1
                                                             bounding-box
                                                             :reversed? true
                                                             :extend-from? false)
        line-edge-top-left-second (line/create-with-extension context
                                                              opposite-line
                                                              corner-top-left top-1
                                                              bounding-box
                                                              :extend-from? false)
        line-edge-top-right-first (line/create-with-extension context
                                                              line
                                                              corner-top-right top-2
                                                              bounding-box
                                                              :reversed? true
                                                              :extend-from? false)
        line-edge-top-right-second (line/create-with-extension context
                                                               opposite-line
                                                               corner-top-right right-1
                                                               bounding-box
                                                               :extend-from? false)
        line-edge-bottom-left-first (line/create-with-extension context
                                                                line
                                                                corner-bottom-left bottom-1
                                                                bounding-box
                                                                :reversed? true
                                                                :extend-from? false)
        line-edge-bottom-left-second (line/create-with-extension context
                                                                 opposite-line
                                                                 corner-bottom-left left-2
                                                                 bounding-box
                                                                 :extend-from? false)
        line-edge-bottom-right-first (line/create-with-extension context
                                                                 line
                                                                 corner-bottom-right right-2
                                                                 bounding-box
                                                                 :reversed? true
                                                                 :extend-from? false)
        line-edge-bottom-right-second (line/create-with-extension context
                                                                  opposite-line
                                                                  corner-bottom-right bottom-2
                                                                  bounding-box
                                                                  :extend-from? false)]
    (post-process/shape
     {:shape [(shape/build-shape
               context
               line-edge-top-left-first
               line-edge-top-left-second
               :clockwise-shortest
               line-edge-top-right-first
               line-edge-top-right-second
               :clockwise-shortest
               line-edge-bottom-right-first
               line-edge-bottom-right-second
               :clockwise-shortest
               line-edge-bottom-left-first
               line-edge-bottom-left-second
               :clockwise-shortest)]
      :edges [{:lines [line-edge-top-left-first
                       line-edge-top-left-second]}
              {:lines [line-edge-top-right-first
                       line-edge-top-right-second]}
              {:lines [line-edge-bottom-left-first
                       line-edge-bottom-left-second]}
              {:lines [line-edge-bottom-right-first
                       line-edge-bottom-right-second]}]}
     context
     properties)))

(defn- cottise-part-properties [variant [base-left base-point base-right] distance band-size reference-line]
  (let [joint-angle 90
        real-distance (/ (+ (:effective-height reference-line)
                            distance)
                         (Math/sin (-> joint-angle
                                       (* Math/PI)
                                       (/ 180)
                                       (/ 2))))
        edge-angle (case variant
                     :top-left 225
                     :top-right 315
                     :bottom-left 135
                     :bottom-right 45)
        delta (/ band-size
                 (Math/sin (-> joint-angle
                               (* Math/PI)
                               (/ 180)
                               (/ 2))))
        dist-vector (v/rotate
                     (v/Vector. real-distance 0)
                     edge-angle)
        band-size-vector (v/rotate
                          (v/Vector. delta 0)
                          edge-angle)
        upper-corner (v/add base-point dist-vector)
        lower-corner (v/add upper-corner band-size-vector)
        [upper-right upper-left] (map #(v/add % dist-vector) [base-left base-right])
        [lower-left lower-right] (map #(v/add % band-size-vector) [upper-left upper-right])]
    {:type :heraldry.ordinary.type/chevron
     :upper [upper-left upper-corner upper-right]
     :lower [lower-left lower-corner lower-right]
     :flip-cottise? true
     :chevron-angle edge-angle
     :joint-angle joint-angle
     :band-size band-size}))

(defmethod cottising/cottise-properties ordinary-type [{:keys [cottise-parts path]
                                                        :as context}
                                                       {:keys [line-length percentage-base
                                                               edge-top-left edge-top-right
                                                               edge-bottom-left edge-bottom-right
                                                               humetty]
                                                        reference-line :line}]
  (when-not (-> (cottising/kind context) name (str/starts-with? "cottise-opposite"))
    (let [distance (interface/get-sanitized-data (c/++ context :distance))
          distance (math/percent-of percentage-base distance)
          thickness (interface/get-sanitized-data (c/++ context :thickness))
          band-size (math/percent-of percentage-base thickness)
          part (get cottise-parts path 0)
          [variant edge] (case part
                           0 [:top-left edge-top-left]
                           1 [:top-right edge-top-right]
                           2 [:bottom-left edge-bottom-left]
                           3 [:bottom-right edge-bottom-right])]
      (post-process/properties
       (-> (cottise-part-properties variant edge distance band-size reference-line)
           (assoc :line-length line-length
                  :percentage-base percentage-base
                  :swap-lines? true
                  :humetty humetty))
       context))))
