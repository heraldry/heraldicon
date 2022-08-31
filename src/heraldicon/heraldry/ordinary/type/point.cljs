(ns heraldicon.heraldry.ordinary.type.point
  (:require
   [clojure.string :as s]
   [heraldicon.context :as c]
   [heraldicon.heraldry.cottising :as cottising]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.ordinary.interface :as ordinary.interface]
   [heraldicon.heraldry.ordinary.post-process :as post-process]
   [heraldicon.heraldry.ordinary.shared :as ordinary.shared]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.path :as path]
   [heraldicon.svg.shape :as shape]))

(def ordinary-type :heraldry.ordinary.type/point)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/point)

(def ^:private variant-choices
  [[:string.option.variant-point-choice/dexter :dexter]
   [:string.option.variant-point-choice/sinister :sinister]])

(def variant-map
  (options/choices->map variant-choices))

(defmethod ordinary.interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil)
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))]
    (ordinary.shared/add-humetty-and-voided
     {:line line-style
      :variant {:type :option.type/choice
                :choices variant-choices
                :default :dexter
                :ui/label :string.option/variant
                :ui/element :ui.element/select}
      :geometry {:width {:type :option.type/range
                         :min 10
                         :max 100
                         :default 50
                         :ui/label :string.option/width}
                 :height {:type :option.type/range
                          :min 10
                          :max 100
                          :default 50
                          :ui/label :string.option/height}
                 :ui/label :string.option/geometry
                 :ui/element :ui.element/geometry}
      :outline? options/plain-outline?-option
      :cottising (cottising/add-cottising context 1)} context)))

(defmethod interface/properties ordinary-type [context]
  (let [{:keys [width height]
         :as parent-environment} (interface/get-parent-environment context)
        variant (interface/get-sanitized-data (c/++ context :variant))
        dexter? (= variant :dexter)
        point-width (interface/get-sanitized-data (c/++ context :geometry :width))
        point-height (interface/get-sanitized-data (c/++ context :geometry :height))
        {:keys [top-left top-right]} (:points parent-environment)
        percentage-base (min width height)
        real-point-width (math/percent-of percentage-base point-width)
        real-point-height (math/percent-of percentage-base point-height)
        corner-point (if dexter?
                       top-left
                       top-right)
        ideal-point-side (v/add corner-point (v/Vector. 0 real-point-height))
        ideal-point-top ((if dexter?
                           v/add
                           v/sub) corner-point (v/Vector. real-point-width 0))
        parent-shape (interface/get-exact-parent-shape context)
        [lower-left lower-right] (v/intersections-with-shape
                                  ideal-point-top ideal-point-side parent-shape :default? true)
        [lower-left lower-right] (if dexter?
                                   [lower-left lower-right]
                                   [lower-right lower-left])
        line-length (v/abs (v/sub lower-left lower-right))]
    (post-process/properties
     {:type ordinary-type
      :lower [lower-left lower-right]
      :point-height real-point-height
      :dexter? dexter?
      :line-length line-length
      :percentage-base percentage-base
      :humetty-percentage-base (:width parent-environment)
      :voided-percentage-base real-point-height}
     context)))

(defmethod interface/environment ordinary-type [context {:keys [dexter?]
                                                         [lower-left lower-right] :lower}]
  (let [{:keys [points]} (interface/get-parent-environment context)
        corner (if dexter?
                 (:top-left points)
                 (:top-right points))
        bounding-box-points [corner
                             lower-left lower-right]]
    (environment/create (bb/from-points bounding-box-points))))

(defmethod interface/render-shape ordinary-type [context {:keys [dexter? line]
                                                          [lower-left lower-right] :lower
                                                          :as properties}]
  (let [{:keys [bounding-box]} (interface/get-parent-environment context)
        line-lower (line/create-with-extension line
                                               (if dexter?
                                                 lower-left
                                                 lower-right)
                                               (if dexter?
                                                 lower-right
                                                 lower-left)
                                               bounding-box
                                               :reversed? (not dexter?)
                                               :context context)]
    (post-process/shape
     {:shape (shape/build-shape
              context
              line-lower
              :clockwise)
      :lines [{:segments [line-lower]}]}
     context
     properties)))

(defmethod cottising/cottise-properties ordinary-type [context
                                                       {:keys [line-length percentage-base humetty]
                                                        [reference-lower-left reference-lower-right] :lower
                                                        dexter? :dexter?
                                                        reference-lower-line :line}]
  (when-not (-> (cottising/kind context) name (s/starts-with? "cottise-opposite"))
    (let [distance (interface/get-sanitized-data (c/++ context :distance))
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
          real-distance (+ (:effective-height reference-lower-line)
                           distance)
          direction (v/sub reference-lower-right reference-lower-left)
          direction (-> (v/Vector. (-> direction :x Math/abs)
                                   (-> direction :y Math/abs))
                        v/normal
                        (cond->
                          dexter? (v/dot (v/Vector. 1 -1))))
          direction-orthogonal (v/orthogonal direction)
          direction-orthogonal (if (neg? (:y direction-orthogonal))
                                 (v/mul direction-orthogonal -1)
                                 direction-orthogonal)
          angle (v/angle-to-point reference-lower-right reference-lower-left)
          dist-vector (v/mul direction-orthogonal real-distance)
          band-size-vector (v/mul direction-orthogonal band-size)
          [upper-left upper-right] (map #(v/add % dist-vector) [reference-lower-left reference-lower-right])
          [lower-left lower-right] (map #(v/add % band-size-vector) [upper-left upper-right])
          ;; swap left and right for cottises
          [upper-left upper-right] [upper-right upper-left]
          [lower-left lower-right] [lower-right lower-left]
          reverse-transform-fn (when-not use-parent-environment?
                                 (fn reverse-transform-fn [v]
                                   (if (instance? v/Vector v)
                                     (-> v
                                         (v/sub upper-left)
                                         (v/rotate (- angle)))
                                     (-> v
                                         (path/translate (- (:x upper-left)) (- (:y upper-left)))
                                         (path/rotate (- angle))))))
          [line opposite-line] [opposite-line line]]
      (post-process/properties
       {:type (if dexter?
                :heraldry.ordinary.type/bend-sinister
                :heraldry.ordinary.type/bend)
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
        :flip-cottise? true
        :line line
        :opposite-line opposite-line
        :humetty humetty}
       context))))
