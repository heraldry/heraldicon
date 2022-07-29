(ns heraldicon.heraldry.ordinary.type.pale
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.cottising :as cottising]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.field.shared :as field.shared]
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

(def ordinary-type :heraldry.ordinary.type/pale)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/pale)

(defmethod ordinary.interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside))]
    (ordinary.shared/add-humetty-and-voided
     {:anchor {:point {:type :option.type/choice
                       :choices (position/anchor-choices
                                 [:fess
                                  :dexter
                                  :sinister
                                  :hoist
                                  :fly
                                  :left
                                  :right])
                       :default :fess
                       :ui/label :string.option/point}
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
        {:keys [top bottom]} (:points parent-environment)
        percentage-base (:width parent-environment)
        band-size (math/percent-of percentage-base size)
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        anchor-point (position/calculate anchor parent-environment :fess)
        left (case (:alignment anchor)
               :left (:x anchor-point)
               :right (- (:x anchor-point) band-size)
               (- (:x anchor-point) (/ band-size 2)))
        right (+ left band-size)
        parent-shape (interface/get-exact-shape parent)
        [left-upper left-lower] (v/intersections-with-shape
                                 (v/Vector. left (:y top)) (v/Vector. left (:y bottom))
                                 parent-shape
                                 :default? true)
        [right-upper right-lower] (v/intersections-with-shape
                                   (v/Vector. right (:y top)) (v/Vector. right (:y bottom))
                                   parent-shape
                                   :default? true)
        line-length (- (:y left-lower) (:y left-upper))
        line (line/resolve-percentages (interface/get-sanitized-data (c/++ context :line))
                                       line-length percentage-base)
        opposite-line (line/resolve-percentages (interface/get-sanitized-data (c/++ context :opposite-line))
                                                line-length percentage-base)]
    {:left [left-upper left-lower]
     :right [right-upper right-lower]
     :band-size band-size
     :line-length line-length
     :line line
     :opposite-line opposite-line}))

(defmethod interface/environment ordinary-type [context]
  (let [parent (interface/parent context)
        {:keys [meta]} (interface/get-environment parent)
        {[left-upper left-lower] :left
         [right-upper right-lower] :right} (interface/get-properties context)
        bounding-box-points [left-upper left-lower
                             right-upper right-lower]]
    (environment/create
     {:paths nil}
     (-> meta
         (dissoc :context)
         (merge {:bounding-box (bb/from-points bounding-box-points)})))))

(defmethod interface/render-shape ordinary-type
  [context]
  (let [parent (interface/parent context)
        {:keys [meta]} (interface/get-environment parent)
        {[left-upper left-lower] :left
         [right-upper right-lower] :right
         band-size :band-size
         line :line
         opposite-line :opposite-line} (interface/get-properties context)
        environment (interface/get-environment context)
        width (:width environment)
        bounding-box (:bounding-box meta)
        right-upper (assoc right-upper :y (:y left-upper))
        {line-left :line
         line-left-start :line-start
         line-left-from :adjusted-from
         line-left-to :adjusted-to
         :as line-left-data} (line/create-with-extension line
                                                         left-upper left-lower
                                                         bounding-box
                                                         :reversed? true
                                                         :context context)
        {line-right :line
         line-right-start :line-start
         line-right-from :adjusted-from
         line-right-to :adjusted-to
         :as line-right-data} (line/create-with-extension opposite-line
                                                          right-upper right-lower
                                                          bounding-box
                                                          :context context)
        shape (ordinary.shared/adjust-shape
               ["M" (v/add line-left-to line-left-start)
                (path/stitch line-left)
                (infinity/path :clockwise
                               [:top :top]
                               [(v/add line-left-from line-left-start)
                                (v/add line-right-from line-right-start)])
                (path/stitch line-right)
                (infinity/path :clockwise
                               [:bottom :bottom]
                               [(v/add line-right-to line-right-start)
                                (v/add line-left-from line-left-start)])
                "z"]
               width
               band-size
               context)]
    {:shape shape
     :lines [{:line line
              :line-from line-left-to
              :line-data [line-left-data]}
             {:line opposite-line
              :line-from line-right-from
              :line-data [line-right-data]}]}))

(defmethod ordinary.interface/render-ordinary ordinary-type [context]
  (ordinary.render/render context))
