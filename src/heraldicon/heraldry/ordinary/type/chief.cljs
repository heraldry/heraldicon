(ns heraldicon.heraldry.ordinary.type.chief
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
   [heraldicon.svg.infinity :as infinity]
   [heraldicon.svg.path :as path]))

(def ordinary-type :heraldry.ordinary.type/chief)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/chief)

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
        {:keys [left right top]} (:points parent-environment)
        percentage-base (:height parent-environment)
        band-size (math/percent-of percentage-base size)
        lower (+ (:y top) band-size)
        parent-shape (interface/get-exact-parent-shape context)
        [lower-left lower-right] (v/intersections-with-shape
                                  (v/Vector. (:x left) lower) (v/Vector. (:x right) lower)
                                  parent-shape :default? true)
        line-length (- (:x lower-right) (:x lower-left))]
    (post-process/properties
     {:type ordinary-type
      :lower [lower-left lower-right]
      :band-size band-size
      :line-length line-length
      :percentage-base percentage-base
      :humetty-percentage-base (:width parent-environment)
      :voided-percentage-base band-size}
     context)))

(defmethod interface/environment ordinary-type [context {[lower-left lower-right] :lower}]
  (let [{:keys [meta points]} (interface/get-parent-environment context)
        {:keys [top-left top-right]} points
        bounding-box-points [top-left top-right
                             lower-left lower-right]]
    (environment/create
     {:paths nil}
     (-> meta
         (dissoc :context)
         (assoc :bounding-box (bb/from-points bounding-box-points))))))

(defmethod interface/render-shape ordinary-type [context {:keys [line]
                                                          [lower-left lower-right] :lower
                                                          :as properties}]
  (let [{:keys [meta]} (interface/get-parent-environment context)
        bounding-box (:bounding-box meta)
        {line-lower :line
         line-lower-start :line-start
         line-lower-from :adjusted-from
         line-lower-to :adjusted-to
         :as line-lower-data} (line/create-with-extension line
                                                          lower-left lower-right
                                                          bounding-box
                                                          :reversed? true
                                                          :context context)]
    (post-process/shape
     {:shape [(path/make-path
               ["M" (v/add line-lower-to line-lower-start)
                (path/stitch line-lower)
                (infinity/clockwise line-lower-from (v/add line-lower-to line-lower-start))
                "z"])]
      :lines [{:line line
               :line-from line-lower-to
               :line-data [line-lower-data]}]}
     context
     properties)))

(defmethod cottising/cottise-properties ordinary-type [context
                                                       {:keys [line-length percentage-base humetty]
                                                        [reference-lower-left reference-lower-right] :lower
                                                        reference-lower-line :line}]
  (when-not (-> (cottising/kind context) name (s/starts-with? "cottise-opposite"))
    (let [distance (interface/get-sanitized-data (c/++ context :distance))
          distance (math/percent-of percentage-base distance)
          thickness (interface/get-sanitized-data (c/++ context :thickness))
          band-size (math/percent-of percentage-base thickness)
          line (line/resolve-percentages (interface/get-sanitized-data (c/++ context :line))
                                         line-length percentage-base)
          opposite-line (line/resolve-percentages (interface/get-sanitized-data (c/++ context :opposite-line))
                                                  line-length percentage-base)
          real-distance (+ (:effective-height reference-lower-line)
                           distance)
          dist-vector (v/Vector. 0 real-distance)
          band-size-vector (v/Vector. 0 band-size)
          [upper-left upper-right] (map #(v/add % dist-vector) [reference-lower-left reference-lower-right])
          [lower-left lower-right] (map #(v/add % band-size-vector) [upper-left upper-right])
          [line opposite-line] [opposite-line line]]
      (post-process/properties
       {:type :heraldry.ordinary.type/fess
        :upper [upper-left upper-right]
        :lower [lower-left lower-right]
        :flip-cottise? true
        :band-size band-size
        :line-length line-length
        :percentage-base percentage-base
        :line line
        :opposite-line opposite-line
        :humetty humetty}
       context))))
