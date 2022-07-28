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
   [heraldicon.svg.infinity :as infinity]
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
        line (interface/get-sanitized-data (c/++ context :line))
        opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
        line (-> line
                 (update-in [:fimbriation :thickness-1] (partial math/percent-of percentage-base))
                 (update-in [:fimbriation :thickness-2] (partial math/percent-of percentage-base)))
        opposite-line (-> opposite-line
                          (update-in [:fimbriation :thickness-1] (partial math/percent-of percentage-base))
                          (update-in [:fimbriation :thickness-2] (partial math/percent-of percentage-base)))
        parent-shape (interface/get-exact-shape parent)
        [upper-left upper-right] (v/intersections-with-shape (v/Vector. 0 upper) (v/Vector. 1 upper) parent-shape)
        [lower-left lower-right] (v/intersections-with-shape (v/Vector. 0 lower) (v/Vector. 1 lower) parent-shape)
        upper-left (or upper-left (v/Vector. (:x left) upper))
        upper-right (or upper-right (v/Vector. (:x right) upper))
        lower-left (or lower-left (v/Vector. (:x left) lower))
        lower-right (or lower-right (v/Vector. (:x right) lower))
        line-height (- (:x upper-right) (:x upper-left))]
    ;; TODO: :base-line will move upper/lower by line-height / 2, difficult because line-height is based
    ;; on the unadjusted upper line
    ;; second thought: maybe the base-line doesn't affect upper/lower at all
    {:upper [upper-left upper-right]
     :lower [lower-left lower-right]
     :band-size band-size
     :line-height line-height
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
  (let [{[upper-left upper-right] :upper
         [lower-left lower-right] :lower
         band-size :band-size
         line :line
         opposite-line :opposite-line} (interface/get-properties context)
        environment (interface/get-environment context)
        width (:width environment)
        shared-start-x (:x upper-left)
        shared-end-x (:x upper-right)
        ;; TODO: not quite right yet
        upper-left (v/Vector. shared-start-x (:y upper-left))
        lower-left (v/Vector. shared-start-x (:y lower-left))
        upper-right (v/Vector. shared-end-x (:y upper-right))
        lower-right (v/Vector. shared-end-x (:y lower-right))
        {line-upper :line
         line-upper-start :line-start
         :as line-upper-data} (line/create line
                                           upper-left upper-right
                                           :context context
                                           :environment environment)
        {line-lower :line
         line-lower-start :line-start
         :as line-lower-data} (line/create opposite-line
                                           lower-left lower-right
                                           :reversed? true
                                           :context context
                                           :environment environment)
        shape (ordinary.shared/adjust-shape
               ["M" (v/add upper-left line-upper-start)
                (path/stitch line-upper)
                (infinity/path :clockwise
                               [:right :right]
                               [(v/add upper-right line-upper-start)
                                (v/add lower-right line-lower-start)])
                (path/stitch line-lower)
                (infinity/path :clockwise
                               [:left :left]
                               [(v/add lower-left line-lower-start)
                                (v/add upper-left line-upper-start)])
                "z"]
               width
               band-size
               context)]
    {:shape shape
     :lines [{:path (path/make-path ["M" (v/add upper-left line-upper-start) (path/stitch line-upper)])
              :line line
              :line-start upper-left
              :line-data [line-upper-data]
              :fimbriation-context (c/++ context :line :fimbriation)}
             {:path (path/make-path ["M" (v/add lower-right line-lower-start) (path/stitch line-lower)])
              :line opposite-line
              :line-start lower-right
              :line-data [line-lower-data]
              :fimbriation-context (c/++ context :opposite-line :fimbriation)}]}))

(defmethod ordinary.interface/render-ordinary ordinary-type [context]
  (ordinary.render/render context))
