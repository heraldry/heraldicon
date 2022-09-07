(ns heraldicon.heraldry.ordinary.type.pall
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
   [heraldicon.heraldry.shared.chevron :as chevron]
   [heraldicon.interface :as interface]
   [heraldicon.math.angle :as angle]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.shape :as shape]))

(def ordinary-type :heraldry.ordinary.type/pall)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/pall)

(defmethod ordinary.interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil)
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside))
        extra-line-style (-> (line/options (c/++ context :extra-line) :inherited-options line-style)
                             (options/override-if-exists [:offset :min] 0)
                             (options/override-if-exists [:base-line] nil)
                             (options/override-if-exists [:fimbriation :alignment :default] :outside))
        origin-point-option {:type :option.type/choice
                             :choices (position/orientation-choices
                                       [:chief
                                        :base
                                        :dexter
                                        :sinister
                                        :hoist
                                        :fly
                                        :top-left
                                        :top
                                        :top-right
                                        :left
                                        :right
                                        :bottom-left
                                        :bottom
                                        :bottom-right
                                        :angle])
                             :default :top
                             :ui/label :string.option/point}
        current-origin-point (options/get-value
                              (interface/get-raw-data (c/++ context :origin :point))
                              origin-point-option)
        orientation-point-option {:type :option.type/choice
                                  :choices (position/orientation-choices
                                            (case current-origin-point
                                              :bottom [:bottom-left
                                                       :bottom
                                                       :bottom-right
                                                       :left
                                                       :right
                                                       :angle]
                                              :top [:top-left
                                                    :top
                                                    :top-right
                                                    :left
                                                    :right
                                                    :angle]
                                              :left [:top-left
                                                     :left
                                                     :bottom-left
                                                     :top
                                                     :bottom
                                                     :angle]
                                              :right [:top-right
                                                      :right
                                                      :bottom-right
                                                      :top
                                                      :bottom
                                                      :angle]
                                              :bottom-left [:bottom-left
                                                            :bottom
                                                            :bottom-right
                                                            :top-left
                                                            :left
                                                            :angle]
                                              :bottom-right [:bottom-left
                                                             :bottom
                                                             :bottom-right
                                                             :right
                                                             :top-right
                                                             :angle]
                                              :top-left [:top-left
                                                         :top
                                                         :top-right
                                                         :left
                                                         :bottom-left
                                                         :angle]
                                              :top-right [:top-left
                                                          :top
                                                          :top-right
                                                          :left
                                                          :bottom-right
                                                          :angle]
                                              [:top-left
                                               :top
                                               :top-right
                                               :left
                                               :right
                                               :bottom-left
                                               :bottom
                                               :bottom-right
                                               :angle]))
                                  :default (case current-origin-point
                                             :bottom :bottom-left
                                             :top :top-right
                                             :left :top-left
                                             :right :bottom-right
                                             :bottom-left :left
                                             :bottom-right :bottom
                                             :top-left :top
                                             :top-right :right
                                             :angle :angle
                                             :bottom-left)
                                  :ui/label :string.option/point}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
    (ordinary.shared/add-humetty-and-voided
     {:anchor {:point {:type :option.type/choice
                       :choices (position/anchor-choices
                                 [:chief
                                  :base
                                  :fess
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
                                  :bottom-right
                                  :angle])
                       :default :fess
                       :ui/label :string.option/point}
               :alignment {:type :option.type/choice
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
                          :ui/step 0.1}
               :ui/label :string.option/anchor
               :ui/element :ui.element/position}
      :origin (cond-> {:point origin-point-option
                       :ui/label :string.charge.attitude/issuant
                       :ui/element :ui.element/position}

                (= current-origin-point
                   :angle) (assoc :angle {:type :option.type/range
                                          :min -180
                                          :max 180
                                          :default 0
                                          :ui/label :string.option/angle})

                (not= current-origin-point
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
      :orientation (cond-> {:point orientation-point-option
                            :ui/label :string.option/orientation
                            :ui/element :ui.element/position}

                     (= current-orientation-point
                        :angle) (assoc :angle {:type :option.type/range
                                               :min 0
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
      :extra-line extra-line-style
      :geometry {:size {:type :option.type/range
                        :min 0.1
                        :max 50
                        :default 20
                        :ui/label :string.option/size
                        :ui/step 0.1}
                 :ui/label :string.option/geometry
                 :ui/element :ui.element/geometry}
      :outline? options/plain-outline?-option
      :cottising (cottising/add-cottising context 3)} context)))

(defmethod interface/properties ordinary-type [context]
  (let [{:keys [width height]
         :as parent-environment} (interface/get-parent-environment context)
        size (interface/get-sanitized-data (c/++ context :geometry :size))
        percentage-base (min width height)
        band-size (math/percent-of percentage-base size)
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        origin (interface/get-sanitized-data (c/++ context :origin))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
        raw-origin (interface/get-raw-data (c/++ context :origin))
        origin (cond-> origin
                 (-> origin
                     :point
                     #{:left
                       :right
                       :top
                       :bottom}) (assoc :offset-x (or (:offset-x raw-origin)
                                                      (:offset-x anchor))
                                        :offset-y (or (:offset-y raw-origin)
                                                      (:offset-y anchor))))
        {direction-anchor-point :real-anchor
         origin-point :real-orientation} (position/calculate-anchor-and-orientation
                                          parent-environment
                                          anchor
                                          origin
                                          0
                                          -90)
        pall-angle (angle/normalize
                    (v/angle-to-point direction-anchor-point
                                      origin-point))
        {anchor-point :real-anchor
         orientation-point :real-orientation} (position/calculate-anchor-and-orientation
                                               parent-environment
                                               anchor
                                               orientation
                                               band-size
                                               pall-angle)
        ;; left/right are based on the chevron view
        [relative-left relative-right] (chevron/arm-diagonals pall-angle anchor-point orientation-point)
        relative-bottom (v/mul (v/add relative-left relative-right) -1)
        angle-left (angle/normalize (v/angle-to-point v/zero relative-left))
        angle-right (angle/normalize (v/angle-to-point v/zero relative-right))
        joint-angle (angle/normalize (- angle-left angle-right))
        delta (/ band-size 2 (Math/sin (-> joint-angle
                                           (* Math/PI)
                                           (/ 180)
                                           (/ 2))))
        offset-bottom (v/rotate
                       (v/Vector. delta 0)
                       pall-angle)
        dx (/ band-size 2)
        dy (/ band-size 2 (Math/tan (-> 180
                                        (- (/ joint-angle 2))
                                        (/ 2)
                                        (* Math/PI)
                                        (/ 180))))
        offset-left (v/rotate
                     (v/Vector. (- dy) dx)
                     pall-angle)
        offset-right (v/rotate
                      (v/Vector. (- dy) (- dx))
                      pall-angle)
        parent-shape (interface/get-exact-parent-shape context)
        corner-bottom (v/add anchor-point offset-bottom)
        corner-left (v/add anchor-point offset-left)
        corner-right (v/add anchor-point offset-right)
        bottom-1 (v/last-intersection-with-shape corner-bottom relative-right
                                                 parent-shape :default? true :relative? true)
        bottom-2 (v/last-intersection-with-shape corner-bottom relative-left
                                                 parent-shape :default? true :relative? true)
        left-1 (v/last-intersection-with-shape corner-left relative-left
                                               parent-shape :default? true :relative? true)
        left-2 (v/last-intersection-with-shape corner-left relative-bottom
                                               parent-shape :default? true :relative? true)
        right-1 (v/last-intersection-with-shape corner-right relative-bottom
                                                parent-shape :default? true :relative? true)
        right-2 (v/last-intersection-with-shape corner-right relative-right
                                                parent-shape :default? true :relative? true)
        line-length (->> (concat (map (fn [v]
                                        (v/sub v corner-bottom))
                                      [bottom-1 bottom-2])
                                 (map (fn [v]
                                        (v/sub v corner-left))
                                      [left-1 left-2])
                                 (map (fn [v]
                                        (v/sub v corner-right))
                                      [right-2 right-2]))
                         (map v/abs)
                         (apply max))]
    (post-process/properties
     {:type ordinary-type
      :fess anchor-point
      :edge-bottom [bottom-1 corner-bottom bottom-2]
      :edge-left [left-1 corner-left left-2]
      :edge-right [right-1 corner-right right-2]
      :band-size band-size
      :line-length line-length
      :percentage-base percentage-base
      :humetty-percentage-base (min (:width parent-environment)
                                    (:height parent-environment))
      :voided-percentage-base band-size
      :num-cottise-parts 3}
     context)))

(defmethod interface/environment ordinary-type [context {:keys [fess]
                                                         [bottom-1 _corner-top bottom-2] :edge-bottom
                                                         [left-1 _corner-left left-2] :edge-left
                                                         [right-1 _corner-right right-2] :edge-right}]
  (let [{:keys [points]} (interface/get-parent-environment context)
        bounding-box-points [bottom-1 bottom-2
                             left-1 left-2
                             right-1 right-2
                             (:bottom points)
                             (:top points)
                             (:left points)
                             (:right points)]]
    ;; TODO: maybe best to inherit the parent environment, unless the pall is couped
    (environment/create (bb/from-points bounding-box-points)
                        {:fess fess})))

(defmethod interface/render-shape ordinary-type [context {:keys [line opposite-line extra-line]
                                                          [bottom-1 corner-bottom bottom-2] :edge-bottom
                                                          [left-1 corner-left left-2] :edge-left
                                                          [right-1 corner-right right-2] :edge-right
                                                          :as properties}]
  (let [{:keys [bounding-box]} (interface/get-parent-environment context)
        line-edge-bottom-first (line/create-with-extension context
                                                           extra-line
                                                           corner-bottom bottom-1
                                                           bounding-box
                                                           :reversed? true
                                                           :extend-from? false)
        line-edge-bottom-second (line/create-with-extension context
                                                            extra-line
                                                            corner-bottom bottom-2
                                                            bounding-box
                                                            :extend-from? false)
        line-edge-left-first (line/create-with-extension context
                                                         opposite-line
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
                                                           line
                                                           corner-right right-2
                                                           bounding-box
                                                           :extend-from? false)]
    ;; TODO: seems to work fine without it, but maybe infinity patching would improve this
    (post-process/shape
     {:shape [(shape/build-shape
               context
               line-edge-bottom-first
               line-edge-bottom-second
               :clockwise-shortest
               line-edge-left-first
               line-edge-left-second
               :clockwise-shortest
               line-edge-right-first
               line-edge-right-second
               :clockwise-shortest)]
      :edges [{:lines [line-edge-bottom-first
                       line-edge-bottom-second]}
              {:lines [line-edge-left-first
                       line-edge-left-second]}
              {:lines [line-edge-right-first
                       line-edge-right-second]}]}
     context
     properties)))

(defn- cottise-part-properties [[base-left base-point base-right] distance band-size reference-line]
  (let [joint-angle (v/angle-between-vectors (v/sub base-right base-point)
                                             (v/sub base-left base-point))
        real-distance (/ (+ (:effective-height reference-line)
                            distance)
                         (Math/sin (-> joint-angle
                                       (* Math/PI)
                                       (/ 180)
                                       (/ 2))))
        edge-angle (+ (v/angle-to-point base-point base-left)
                      (/ joint-angle 2))
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
                                                               edge-bottom edge-left edge-right
                                                               humetty]
                                                        reference-line :line
                                                        reference-opposite-line :opposite-line
                                                        reference-extra-line :extra-line}]
  (when-not (-> (cottising/kind context) name (s/starts-with? "cottise-opposite"))
    (let [distance (interface/get-sanitized-data (c/++ context :distance))
          distance (math/percent-of percentage-base distance)
          thickness (interface/get-sanitized-data (c/++ context :thickness))
          band-size (math/percent-of percentage-base thickness)
          part (get cottise-parts path 0)
          [edge ref-line] (case part
                            0 [edge-right reference-line]
                            1 [edge-left reference-opposite-line]
                            2 [edge-bottom reference-extra-line])]
      (post-process/properties
       (-> (cottise-part-properties edge distance band-size ref-line)
           (assoc :line-length line-length
                  :percentage-base percentage-base
                  :swap-lines? true
                  :humetty humetty))
       context))))
