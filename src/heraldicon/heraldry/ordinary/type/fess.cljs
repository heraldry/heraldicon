(ns heraldicon.heraldry.ordinary.type.fess
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.cottising :as cottising]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.ordinary.interface :as ordinary.interface]
   [heraldicon.heraldry.ordinary.render :as ordinary.render]
   [heraldicon.heraldry.ordinary.shared :as ordinary.shared]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.path :as path]))

(def ordinary-type :heraldry.ordinary.type/fess)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/fess)

(defmethod ordinary.interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside))]
    (ordinary.shared/add-humetty-and-voided
     {:anchor {:point {:type :option.type/choice
                       :choices (position/anchor-choices
                                 [:fess
                                  :chief
                                  :base
                                  :honour
                                  :nombril
                                  :top
                                  :center
                                  :bottom])
                       :default :fess
                       :ui/label :string.option/point}
               :alignment {:type :option.type/choice
                           :choices position/alignment-choices
                           :default :middle
                           :ui/label :string.option/alignment
                           :ui/element :ui.element/radio-select}
               :offset-y {:type :option.type/range
                          :min -75
                          :max 75
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
      :cottising (cottising/add-cottising context 2)} context)))

(defmethod interface/properties ordinary-type [context]
  (let [parent (interface/parent context)
        parent-environment (interface/get-environment parent)
        size (interface/get-sanitized-data (c/++ context :geometry :size))
        {:keys [left right]} (:points parent-environment)
        percentage-base (:height parent-environment)
        band-size (math/percent-of percentage-base size)
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        anchor-point (position/calculate anchor parent-environment :fess)
        upper (case (:alignment anchor)
                :left (:y anchor-point)
                :right (- (:y anchor-point) band-size)
                (- (:y anchor-point) (/ band-size 2)))
        lower (+ upper band-size)
        parent-shape (interface/get-exact-shape parent)
        [upper-left upper-right] (v/intersections-with-shape
                                  (v/Vector. (:x left) upper) (v/Vector. (:x right) upper)
                                  parent-shape :default? true)
        [lower-left lower-right] (v/intersections-with-shape
                                  (v/Vector. (:x left) lower) (v/Vector. (:x right) lower)
                                  parent-shape :default? true)
        line-length (- (:x upper-right) (:x upper-left))
        line (line/resolve-percentages (interface/get-sanitized-data (c/++ context :line))
                                       line-length percentage-base)
        opposite-line (line/resolve-percentages (interface/get-sanitized-data (c/++ context :opposite-line))
                                                line-length percentage-base)]
    {:upper [upper-left upper-right]
     :lower [lower-left lower-right]
     :band-size band-size
     :line-height line-length
     :line line
     :opposite-line opposite-line}))

(defmethod interface/environment ordinary-type [context]
  (let [parent (interface/parent context)
        {:keys [meta]} (interface/get-environment parent)
        {[upper-left upper-right] :upper
         [lower-left lower-right] :lower} (interface/get-properties context)
        bounding-box-points [upper-left upper-right
                             lower-left lower-right]]
    (environment/create
     {:paths nil}
     (-> meta
         (dissoc :context)
         (merge {:bounding-box (bb/from-points bounding-box-points)})))))

(defmethod interface/render-shape ordinary-type
  [context]
  (let [parent (interface/parent context)
        {:keys [meta]} (interface/get-environment parent)
        {[upper-left upper-right] :upper
         [lower-left lower-right] :lower
         band-size :band-size
         line :line
         opposite-line :opposite-line} (interface/get-properties context)
        environment (interface/get-environment context)
        width (:width environment)
        bounding-box (:bounding-box meta)
        lower-left (assoc lower-left :x (:x upper-left))
        {line-upper :line
         line-upper-start :line-start
         line-upper-from :adjusted-from
         :as line-upper-data} (line/create-with-extension line
                                                          upper-left upper-right
                                                          bounding-box
                                                          :context context)
        {line-lower :line
         line-lower-start :line-start
         line-lower-to :adjusted-to
         :as line-lower-data} (line/create-with-extension opposite-line
                                                          lower-left lower-right
                                                          bounding-box
                                                          :reversed? true
                                                          :context context)
        shape (ordinary.shared/adjust-shape
               ["M" (v/add line-upper-from line-upper-start)
                (path/stitch line-upper)
                "L" (v/add line-lower-to line-lower-start)
                (path/stitch line-lower)
                "z"]
               width
               band-size
               context)]
    {:shape shape
     :lines [{:line line
              :line-from line-upper-from
              :line-data [line-upper-data]}
             {:line opposite-line
              :line-from line-lower-to
              :line-data [line-lower-data]}]}))

(defmethod ordinary.interface/render-ordinary ordinary-type [context]
  (ordinary.render/render context))
