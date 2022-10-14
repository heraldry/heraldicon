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
  (let [{:keys [points width height]} (interface/get-parent-field-environment context)
        size (interface/get-sanitized-data (c/++ context :geometry :size))
        {:keys [left right bottom]} points
        percentage-base height
        band-size (math/percent-of percentage-base size)
        upper (- (:y bottom) band-size)
        parent-shape (interface/get-parent-field-shape context)
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
      :humetty-percentage-base width
      :voided-percentage-base band-size}
     context)))

(defmethod interface/environment ordinary-type [context]
  (let [{[upper-left upper-right] :upper} (interface/get-properties context)
        {:keys [points]} (interface/get-parent-field-environment context)
        {:keys [bottom]} points
        bounding-box-points [upper-left upper-right
                             bottom]]
    (environment/create (bb/from-points bounding-box-points))))

(defmethod interface/render-shape ordinary-type [context]
  (let [{:keys [line]
         [upper-left upper-right] :upper
         :as properties} (interface/get-properties context)
        {:keys [bounding-box]} (interface/get-parent-field-environment context)
        line-upper (line/create-with-extension context
                                               line
                                               upper-left upper-right
                                               bounding-box)]
    (post-process/shape
     {:shape [(shape/build-shape
               context
               line-upper
               :clockwise)]
      :edges [{:lines [line-upper]}]}
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
        :humetty humetty}
       context))))

(defmethod interface/parent-field-environment ordinary-type [context]
  (interface/get-environment (interface/parent context)))

(prefer-method interface/parent-field-environment ordinary-type :heraldry/ordinary)

(defmethod interface/parent-field-shape ordinary-type [context]
  (interface/get-exact-shape (interface/parent context)))

(prefer-method interface/parent-field-shape ordinary-type :heraldry/ordinary)
