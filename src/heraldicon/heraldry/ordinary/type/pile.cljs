(ns heraldicon.heraldry.ordinary.type.pile
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
   [heraldicon.heraldry.shared.pile :as pile]
   [heraldicon.interface :as interface]
   [heraldicon.math.angle :as angle]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.shape :as shape]))

(def ordinary-type :heraldry.ordinary.type/pile)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/pile)

(def ^:private size-mode-choices
  [[:string.option.size-mode-choice/thickness :thickness]
   [:string.option.size-mode-choice/angle :angle]])

(def size-mode-map
  (options/choices->map size-mode-choices))

(def ^:private orientation-type-choices
  [[:string.option.orientation-type-choice/edge :edge]
   [:string.option.orientation-type-choice/orientation-point :point]])

(defmethod ordinary.interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil)
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside))
        anchor-point-option {:type :option.type/choice
                             :choices (position/anchor-choices
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
                                        :center
                                        :right
                                        :bottom-left
                                        :bottom
                                        :bottom-right])
                             :default :top
                             :ui/label :string.option/point}
        current-anchor-point (options/get-value
                              (interface/get-raw-data (c/++ context :anchor :point))
                              anchor-point-option)
        orientation-point-option {:type :option.type/choice
                                  :choices (position/orientation-choices
                                            (filter
                                             #(not= % current-anchor-point)
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
                                              :hoist
                                              :fly
                                              :angle]))
                                  :default :fess
                                  :ui/label :string.option/point}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)
        size-mode-option {:type :option.type/choice
                          :choices size-mode-choices
                          :default :thickness
                          :ui/label :string.option/size-mode
                          :ui/element :ui.element/radio-select}
        current-size-mode (options/get-value
                           (interface/get-raw-data (c/++ context :geometry :size-mode))
                           size-mode-option)]
    (ordinary.shared/add-humetty-and-voided
     {:adapt-to-ordinaries? {:type :option.type/boolean
                             :default true
                             :ui/label :string.option/adapt-to-ordinaries?}
      :anchor {:point anchor-point-option
               :alignment {:type :option.type/choice
                           :choices position/alignment-choices
                           :default :middle
                           :ui/label :string.option/alignment
                           :ui/element :ui.element/radio-select}
               :offset-x {:type :option.type/range
                          :min -50
                          :max 50
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
                                               :min (cond
                                                      (#{:top-left
                                                         :top-right
                                                         :bottom-left
                                                         :bottom-right} current-anchor-point) 0
                                                      :else -90)
                                               :max 90
                                               :default (cond
                                                          (#{:top-left
                                                             :top-right
                                                             :bottom-left
                                                             :bottom-right} current-anchor-point) 45
                                                          :else 0)
                                               :ui/label :string.option/angle})

                     (not= current-orientation-point
                           :angle) (assoc :offset-x {:type :option.type/range
                                                     :min -50
                                                     :max 50
                                                     :default 0
                                                     :ui/label :string.option/offset-x
                                                     :ui/step 0.1}
                                          :offset-y {:type :option.type/range
                                                     :min -75
                                                     :max 75
                                                     :default 0
                                                     :ui/label :string.option/offset-y
                                                     :ui/step 0.1}
                                          :type {:type :option.type/choice
                                                 :choices orientation-type-choices
                                                 :default :edge
                                                 :ui/label :string.render-options/mode
                                                 :ui/element :ui.element/radio-select}))
      :line line-style
      :opposite-line opposite-line-style
      :geometry {:size-mode size-mode-option
                 :size {:type :option.type/range
                        :min 5
                        :max 120
                        :default (case current-size-mode
                                   :thickness 75
                                   30)
                        :ui/label :string.option/size
                        :ui/step 0.1}
                 :stretch {:type :option.type/range
                           :min 0.33
                           :max 2
                           :default 0.85
                           :ui/label :string.option/stretch
                           :ui/step 0.01}
                 :ui/label :string.option/geometry
                 :ui/element :ui.element/geometry}
      :outline? options/plain-outline?-option
      :cottising (cottising/add-cottising context 1)} context)))

(defmethod interface/properties ordinary-type [context]
  (let [{:keys [width height]
         :as parent-environment} (interface/get-parent-field-environment context)
        geometry (interface/get-sanitized-data (c/++ context :geometry))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
        percentage-base (if (#{:left :right :dexter :sinister} (:point anchor))
                          height
                          width)
        parent-shape (interface/get-parent-field-shape context)
        {anchor-point :anchor
         point :point
         thickness :thickness} (pile/calculate-properties
                                parent-environment
                                parent-shape
                                anchor
                                (cond-> orientation
                                  (#{:top-right
                                     :right
                                     :bottom-left} (:point anchor)) (update :angle #(when %
                                                                                      (- %))))
                                geometry
                                percentage-base
                                (case (:point anchor)
                                  :top-left 0
                                  :top 90
                                  :top-right 180
                                  :left 0
                                  :right 180
                                  :bottom-left 0
                                  :bottom -90
                                  :bottom-right 180
                                  0))
        pile-angle (angle/normalize (v/angle-to-point point anchor-point))
        {left-point :left
         right-point :right} (pile/diagonals anchor-point point thickness)
        intersection-left (v/last-intersection-with-shape point left-point parent-shape :default? true)
        intersection-right (v/last-intersection-with-shape point right-point parent-shape :default? true)
        joint-angle (angle/normalize (v/angle-between-vectors (v/sub intersection-left point)
                                                              (v/sub intersection-right point)))
        line-length (apply max (map (fn [v]
                                      (-> v
                                          (v/sub v point)
                                          v/abs))
                                    [intersection-left
                                     intersection-right]))]
    (post-process/properties
     {:type ordinary-type
      :upper [intersection-left point intersection-right]
      :pile-angle pile-angle
      :joint-angle joint-angle
      :thickness thickness
      :line-length line-length
      :percentage-base percentage-base
      :humetty-percentage-base (min width height)
      :voided-percentage-base (/ thickness 2)}
     context)))

(defmethod interface/environment ordinary-type [context]
  (let [{[left point right] :upper} (interface/get-properties context)
        ;; TODO: needs to be improved
        bounding-box-points [point left right]]
    (environment/create (bb/from-points bounding-box-points))))

(defmethod interface/render-shape ordinary-type [context]
  (let [{:keys [line opposite-line]
         [left point right] :upper
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
               :clockwise-shortest)]
      :edges [{:lines [line-left line-right]}]}
     context
     properties)))

(defmethod cottising/cottise-properties ordinary-type [context
                                                       {:keys [line-length percentage-base
                                                               pile-angle joint-angle humetty]
                                                        [reference-left reference-point reference-right] :upper
                                                        reference-line :line}]
  (when-not (-> (cottising/kind context) name (s/starts-with? "cottise-opposite"))
    (let [distance (interface/get-sanitized-data (c/++ context :distance))
          distance (math/percent-of percentage-base distance)
          thickness (interface/get-sanitized-data (c/++ context :thickness))
          band-size (math/percent-of percentage-base thickness)
          [base-corner base-left base-right] [reference-point reference-left reference-right]
          real-distance (/ (+ (:effective-height reference-line)
                              distance)
                           (Math/sin (-> joint-angle
                                         (* Math/PI)
                                         (/ 180)
                                         (/ 2))))
          delta (/ band-size
                   (Math/sin (-> joint-angle
                                 (* Math/PI)
                                 (/ 180)
                                 (/ 2))))
          dist-vector (v/rotate
                       (v/Vector. real-distance 0)
                       pile-angle)
          band-size-vector (v/rotate
                            (v/Vector. delta 0)
                            pile-angle)
          lower-corner (v/sub base-corner dist-vector)
          upper-corner (v/sub lower-corner band-size-vector)
          [lower-left lower-right] (map #(v/sub % dist-vector) [base-left base-right])
          [upper-left upper-right] (map #(v/sub % band-size-vector) [lower-left lower-right])]
      (post-process/properties
       {:type :heraldry.ordinary.type/chevron
        :upper [upper-left upper-corner upper-right]
        :lower [lower-left lower-corner lower-right]
        :chevron-angle pile-angle
        :joint-angle joint-angle
        :band-size band-size
        :line-length line-length
        :percentage-base percentage-base
        :humetty humetty}
       context))))
