(ns heraldicon.heraldry.ordinary.type.pale
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
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.shape :as shape]))

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
  (let [parent-environment (interface/get-parent-environment context)
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
        parent-shape (interface/get-exact-parent-shape context)
        [left-upper left-lower] (v/intersections-with-shape
                                 (v/Vector. left (:y top)) (v/Vector. left (:y bottom))
                                 parent-shape
                                 :default? true)
        [right-upper right-lower] (v/intersections-with-shape
                                   (v/Vector. right (:y top)) (v/Vector. right (:y bottom))
                                   parent-shape
                                   :default? true)
        start-y (min (:y left-upper) (:y right-upper))
        left-upper (assoc left-upper :y start-y)
        right-upper (assoc right-upper :y start-y)
        line-length (max (v/abs (v/sub left-upper left-lower))
                         (v/abs (v/sub left-lower right-lower)))]
    (post-process/properties
     {:type ordinary-type
      :left [left-upper left-lower]
      :right [right-upper right-lower]
      :band-size band-size
      :line-length line-length
      :percentage-base percentage-base
      :humetty-percentage-base (:height parent-environment)
      :voided-percentage-base band-size}
     context)))

(defmethod interface/environment ordinary-type [_context {[left-upper left-lower] :left
                                                          [right-upper right-lower] :right}]
  (let [bounding-box-points [left-upper left-lower
                             right-upper right-lower]]
    (environment/create (bb/from-points bounding-box-points))))

(defmethod interface/render-shape ordinary-type [context {:keys [line opposite-line]
                                                          [left-upper left-lower] :left
                                                          [right-upper right-lower] :right
                                                          :as properties}]
  (let [{:keys [bounding-box]} (interface/get-parent-environment context)
        line-left (line/create-with-extension context
                                              line
                                              left-upper left-lower
                                              bounding-box
                                              :reversed? true)
        line-right (line/create-with-extension context
                                               opposite-line
                                               right-upper right-lower
                                               bounding-box)]
    (post-process/shape
     {:shape [(shape/build-shape
               context
               line-left
               :clockwise-shortest
               line-right
               :clockwise-shortest)]
      :edges [{:lines [line-left]}
              {:lines [line-right]}]}
     context
     properties)))

(defmethod cottising/cottise-properties ordinary-type [context
                                                       {:keys [line-length percentage-base humetty]
                                                        [reference-left-upper reference-left-lower] :left
                                                        [reference-right-upper reference-right-lower] :right
                                                        reference-left-line :line
                                                        reference-right-line :opposite-line}]
  (let [kind (cottising/kind context)
        distance (interface/get-sanitized-data (c/++ context :distance))
        distance (math/percent-of percentage-base distance)
        thickness (interface/get-sanitized-data (c/++ context :thickness))
        band-size (math/percent-of percentage-base thickness)
        opposite? (-> kind name (s/starts-with? "cottise-opposite"))
        reference-line (if opposite?
                         reference-right-line
                         reference-left-line)
        real-distance (+ (:effective-height reference-line)
                         distance)
        [base-upper base-lower] (if opposite?
                                  [reference-right-upper reference-right-lower]
                                  [reference-left-upper reference-left-lower])
        dist-vector (v/Vector. real-distance 0)
        band-size-vector (v/Vector. band-size 0)
        add-fn (if opposite?
                 v/add
                 v/sub)
        [first-upper first-lower] (map #(add-fn % dist-vector) [base-upper base-lower])
        [second-upper second-lower] (map #(add-fn % band-size-vector) [first-upper first-lower])
        [left-upper left-lower
         right-upper right-lower] (if opposite?
                                    [first-upper first-lower
                                     second-upper second-lower]
                                    [second-upper second-lower
                                     first-upper first-lower])]
    (post-process/properties
     {:type ordinary-type
      :left [left-upper left-lower]
      :right [right-upper right-lower]
      :band-size band-size
      :line-length line-length
      :percentage-base percentage-base
      :swap-lines? opposite?
      :humetty humetty}
     context)))
