(ns heraldicon.heraldry.ordinary.type.base
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
   [heraldicon.svg.shape :as shape]))

(def ordinary-type :heraldry.ordinary.type/base)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/base)

(defmethod ordinary.interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))]
    (ordinary.shared/add-humetty-and-voided
     {:line line-style
      :geometry {:size {:type :option.type/range
                        :min 0.1
                        :max 75
                        :default 25
                        :ui/label :string.option/size
                        :ui/step 0.1}
                 :ui/label :string.option/geometry
                 :ui/element :ui.element/geometry}
      :outline? options/plain-outline?-option
      :cottising (cottising/add-cottising context 1)} context)))

(defmethod interface/properties ordinary-type [context]
  (let [parent-environment (interface/get-parent-environment context)
        size (interface/get-sanitized-data (c/++ context :geometry :size))
        {:keys [left right bottom]} (:points parent-environment)
        percentage-base (:height parent-environment)
        band-size (math/percent-of percentage-base size)
        upper (- (:y bottom) band-size)
        parent-shape (interface/get-exact-parent-shape context)
        [upper-left upper-right] (v/intersections-with-shape
                                  (v/Vector. (:x left) upper) (v/Vector. (:x right) upper)
                                  parent-shape :default? true)
        line-length (- (:x upper-right) (:x upper-left))]
    (post-process/properties
     {:type ordinary-type
      :upper [upper-left upper-right]
      :band-size band-size
      :line-length line-length
      :percentage-base percentage-base
      :humetty-percentage-base (:width parent-environment)
      :voided-percentage-base band-size}
     context)))

(defmethod interface/environment ordinary-type [context {[upper-left upper-right] :upper}]
  (let [{:keys [points]} (interface/get-parent-environment context)
        {:keys [bottom]} points
        bounding-box-points [upper-left upper-right
                             bottom]]
    (environment/create (bb/from-points bounding-box-points))))

(defmethod interface/render-shape ordinary-type [context {:keys [line]
                                                          [upper-left upper-right] :upper
                                                          :as properties}]
  (let [{:keys [bounding-box]} (interface/get-parent-environment context)
        line-upper (line/create-with-extension line
                                               upper-left upper-right
                                               bounding-box
                                               :context context)]
    (post-process/shape
     {:shape [(shape/build-shape
               context
               line-upper
               :clockwise)]
      :lines [{:segments [line-upper]}]}
     context
     properties)))

(defmethod cottising/cottise-properties ordinary-type [context
                                                       {:keys [line-length percentage-base humetty]
                                                        [reference-upper-left reference-upper-right] :upper
                                                        reference-upper-line :line}]
  (when-not (-> (cottising/kind context) name (s/starts-with? "cottise-opposite"))
    (let [distance (interface/get-sanitized-data (c/++ context :distance))
          distance (math/percent-of percentage-base distance)
          thickness (interface/get-sanitized-data (c/++ context :thickness))
          band-size (math/percent-of percentage-base thickness)
          line (line/resolve-percentages (interface/get-sanitized-data (c/++ context :line))
                                         line-length percentage-base)
          opposite-line (line/resolve-percentages (interface/get-sanitized-data (c/++ context :opposite-line))
                                                  line-length percentage-base)
          real-distance (+ (:effective-height reference-upper-line)
                           distance)
          dist-vector (v/Vector. 0 real-distance)
          band-size-vector (v/Vector. 0 band-size)
          [lower-left lower-right] (map #(v/sub % dist-vector) [reference-upper-left reference-upper-right])
          [upper-left upper-right] (map #(v/sub % band-size-vector) [lower-left lower-right])]
      (post-process/properties
       {:type :heraldry.ordinary.type/fess
        :upper [upper-left upper-right]
        :lower [lower-left lower-right]
        :band-size band-size
        :line-length line-length
        :percentage-base percentage-base
        :line line
        :opposite-line opposite-line
        :humetty humetty}
       context))))
