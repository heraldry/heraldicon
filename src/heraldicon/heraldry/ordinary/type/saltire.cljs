(ns heraldicon.heraldry.ordinary.type.saltire
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
   [heraldicon.heraldry.shared.saltire :as saltire]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.shape :as shape]))

(def ordinary-type :heraldry.ordinary.type/saltire)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/saltire)

(defmethod ordinary.interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil)
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside))
        orientation-point-option {:type :option.type/choice
                                  :choices (position/orientation-choices
                                            [:top-left
                                             :top-right
                                             :bottom-left
                                             :bottom-right
                                             :angle])
                                  :default :top-left
                                  :ui/label :string.option/point}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
    ;; TODO: perhaps there should be anchor options for the corners?
    ;; so one can align fro top-left to bottom-right
    (ordinary.shared/add-humetty-and-voided
     {:ignore-ordinary-impact? {:type :option.type/boolean
                                :default false
                                :ui/label :string.option/ignore-ordinary-impact?}
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
      :orientation (cond-> {:point orientation-point-option
                            :ui/label :string.option/orientation
                            :ui/element :ui.element/position}

                     (= current-orientation-point
                        :angle) (assoc :angle {:type :option.type/range
                                               :min 10
                                               :max 80
                                               :default 45
                                               :ui/label :string.option/angle})

                     (not= current-orientation-point
                           :angle) (assoc :alignment {:type :option.type/choice
                                                      :choices position/alignment-choices
                                                      :default :middle
                                                      :ui/label :string.option/alignment
                                                      :ui/element :ui.element/radio-select}
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
      :cottising (cottising/add-cottising context 1)} context)))

(defmethod interface/properties ordinary-type [context]
  (let [{:keys [width height]
         :as parent-environment} (interface/get-parent-field-environment context)
        size (interface/get-sanitized-data (c/++ context :geometry :size))
        percentage-base (min width
                             height)
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
        [relative-top-left relative-top-right
         relative-bottom-left relative-bottom-right] (saltire/arm-diagonals anchor-point orientation-point)
        parent-shape (interface/get-parent-field-shape context)
        angle-bottom-right (v/angle-to-point v/zero relative-bottom-right)
        angle (-> angle-bottom-right (* Math/PI) (/ 180))
        dx (/ band-size 2 (Math/sin angle))
        dy (/ band-size 2 (Math/cos angle))
        corner-top (v/add anchor-point (v/Vector. 0 (- dy)))
        corner-bottom (v/add anchor-point (v/Vector. 0 dy))
        corner-left (v/add anchor-point (v/Vector. (- dx) 0))
        corner-right (v/add anchor-point (v/Vector. dx 0))
        top-1 (v/last-intersection-with-shape corner-top relative-top-left
                                              parent-shape :default? true :relative? true)
        top-2 (v/last-intersection-with-shape corner-top relative-top-right
                                              parent-shape :default? true :relative? true)
        bottom-1 (v/last-intersection-with-shape corner-bottom relative-bottom-right
                                                 parent-shape :default? true :relative? true)
        bottom-2 (v/last-intersection-with-shape corner-bottom relative-bottom-left
                                                 parent-shape :default? true :relative? true)
        left-1 (v/last-intersection-with-shape corner-left relative-bottom-left
                                               parent-shape :default? true :relative? true)
        left-2 (v/last-intersection-with-shape corner-left relative-top-left
                                               parent-shape :default? true :relative? true)
        right-1 (v/last-intersection-with-shape corner-right relative-top-right
                                                parent-shape :default? true :relative? true)
        right-2 (v/last-intersection-with-shape corner-right relative-bottom-right
                                                parent-shape :default? true :relative? true)
        line-length (->> (concat (map (fn [v]
                                        (v/sub v corner-top))
                                      [top-1 top-2])
                                 (map (fn [v]
                                        (v/sub v corner-bottom))
                                      [bottom-1 bottom-2])
                                 (map (fn [v]
                                        (v/sub v corner-left))
                                      [left-1 left-2])
                                 (map (fn [v]
                                        (v/sub v corner-right))
                                      [right-1 right-2]))
                         (map v/abs)
                         (apply max))]
    (post-process/properties
     {:type ordinary-type
      :edge-top [top-1 corner-top top-2]
      :edge-bottom [bottom-1 corner-bottom bottom-2]
      :edge-left [left-1 corner-left left-2]
      :edge-right [right-1 corner-right right-2]
      :band-size band-size
      :line-length line-length
      :percentage-base percentage-base
      :humetty-percentage-base percentage-base
      :voided-percentage-base band-size
      :num-cottise-parts 4}
     context)))

(defmethod interface/environment ordinary-type [context {[top-1 corner-top top-2] :edge-top
                                                         [bottom-1 corner-bottom bottom-2] :edge-bottom
                                                         [left-1 corner-left left-2] :edge-left
                                                         [right-1 corner-right right-2] :edge-right}]
  (let [{:keys [points]} (interface/get-parent-field-environment context)
        bounding-box-points [top-1 top-2
                             bottom-1 bottom-2
                             left-1 left-2
                             right-1 right-2
                             (:bottom points)
                             (:top points)
                             (:left points)
                             (:right points)]]
    ;; TODO: maybe best to inherit the parent environment, unless the saltire is couped
    (environment/create (bb/from-points bounding-box-points)
                        {:fess (v/avg corner-top
                                      corner-bottom
                                      corner-left
                                      corner-right)})))

(defmethod interface/render-shape ordinary-type [context {:keys [line opposite-line]
                                                          [top-1 corner-top top-2] :edge-top
                                                          [bottom-1 corner-bottom bottom-2] :edge-bottom
                                                          [left-1 corner-left left-2] :edge-left
                                                          [right-1 corner-right right-2] :edge-right
                                                          :as properties}]
  (let [{:keys [bounding-box]} (interface/get-parent-field-environment context)
        line-edge-top-first (line/create-with-extension context
                                                        line
                                                        corner-top top-1
                                                        bounding-box
                                                        :reversed? true
                                                        :extend-from? false)
        line-edge-top-second (line/create-with-extension context
                                                         opposite-line
                                                         corner-top top-2
                                                         bounding-box
                                                         :extend-from? false)
        line-edge-left-first (line/create-with-extension context
                                                         line
                                                         corner-left left-1
                                                         bounding-box
                                                         :reversed? true
                                                         :extend-from? false)
        line-edge-left-second (line/create-with-extension context
                                                          opposite-line
                                                          corner-left left-2
                                                          bounding-box
                                                          :extend-from? false)
        line-edge-right-first (line/create-with-extension context
                                                          line
                                                          corner-right right-1
                                                          bounding-box
                                                          :reversed? true
                                                          :extend-from? false)
        line-edge-right-second (line/create-with-extension context
                                                           opposite-line
                                                           corner-right right-2
                                                           bounding-box
                                                           :extend-from? false)
        line-edge-bottom-first (line/create-with-extension context
                                                           line
                                                           corner-bottom bottom-1
                                                           bounding-box
                                                           :reversed? true
                                                           :extend-from? false)
        line-edge-bottom-second (line/create-with-extension context
                                                            opposite-line
                                                            corner-bottom bottom-2
                                                            bounding-box
                                                            :extend-from? false)]
    (post-process/shape
     {:shape [(shape/build-shape
               context
               line-edge-top-first
               line-edge-top-second
               :clockwise-shortest
               line-edge-right-first
               line-edge-right-second
               :clockwise-shortest
               line-edge-bottom-first
               line-edge-bottom-second
               :clockwise-shortest
               line-edge-left-first
               line-edge-left-second
               :clockwise-shortest)]
      :edges [{:lines [line-edge-top-first
                       line-edge-top-second]}
              {:lines [line-edge-left-first
                       line-edge-left-second]}
              {:lines [line-edge-right-first
                       line-edge-right-second]}
              {:lines [line-edge-bottom-first
                       line-edge-bottom-second]}]}
     context
     properties)))

(defn- cottise-part-properties [variant [base-left base-point base-right] distance band-size reference-line]
  (let [joint-angle (v/angle-between-vectors (v/sub base-right base-point)
                                             (v/sub base-left base-point))
        real-distance (/ (+ (:effective-height reference-line)
                            distance)
                         (Math/sin (-> joint-angle
                                       (* Math/PI)
                                       (/ 180)
                                       (/ 2))))
        edge-angle (case variant
                     :top -90
                     :right 0
                     :bottom 90
                     :left 180)
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
                                                               edge-top edge-left edge-right edge-bottom
                                                               humetty]
                                                        reference-line :line}]
  (when-not (-> (cottising/kind context) name (s/starts-with? "cottise-opposite"))
    (let [distance (interface/get-sanitized-data (c/++ context :distance))
          distance (math/percent-of percentage-base distance)
          thickness (interface/get-sanitized-data (c/++ context :thickness))
          band-size (math/percent-of percentage-base thickness)
          part (get cottise-parts path 0)
          [variant edge] (case part
                           0 [:top edge-top]
                           1 [:right edge-right]
                           2 [:bottom edge-bottom]
                           3 [:left edge-left])]
      (post-process/properties
       (-> (cottise-part-properties variant edge distance band-size reference-line)
           (assoc :line-length line-length
                  :percentage-base percentage-base
                  :swap-lines? true
                  :humetty humetty))
       context))))
