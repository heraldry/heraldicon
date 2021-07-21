(ns heraldry.coat-of-arms.ordinary.type.chief
  (:require [heraldry.coat-of-arms.field.shared :as field-shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.interface :as interface]
            [heraldry.util :as util]))

(def ordinary-type :heraldry.ordinary.type/chief)

(defmethod ordinary-interface/display-name ordinary-type [_] "Chief")

(defmethod ordinary-interface/render-ordinary ordinary-type
  [path _parent-path environment {:keys [override-real-start
                                         override-real-end
                                         override-shared-start-x] :as context}]
  (let [line (interface/get-sanitized-data (conj path :line) context)
        size (interface/get-sanitized-data (conj path :geometry :size) context)
        ;; cottising (interface/get-sanitized-data (conj path :cottising) context)
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (conj path :outline?) context))
        points (:points environment)
        top (:top points)
        top-left (:top-left points)
        left (:left points)
        right (:right points)
        height (:height environment)
        band-height (-> size
                        ((util/percent-of height)))
        row (+ (:y top) band-height)
        row-left (v/v (:x left) row)
        row-right (v/v (:x right) row)
        [row-real-left _row-real-right] (v/environment-intersections
                                         row-left
                                         row-right
                                         environment)
        shared-start-x (or override-shared-start-x
                           (- (:x row-real-left)
                              30))
        real-start (or override-real-start
                       (-> row-left :x (- shared-start-x)))
        real-end (or override-real-end
                     (-> row-right :x (- shared-start-x)))
        shared-end-x (+ real-end 30)
        row-left (v/v shared-start-x (:y row-left))
        row-right (v/v shared-end-x (:y row-right))
        line (-> line
                 (update-in [:fimbriation :thickness-1] (util/percent-of height))
                 (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        {line-reversed :line
         line-reversed-start :line-start
         line-reversed-min :line-min
         :as line-reversed-data} (line/create line
                                              row-left row-right
                                              :reversed? true
                                              :real-start real-start
                                              :real-end real-end
                                              :context context
                                              :environment environment)
        part [["M" (v/+ row-right
                        line-reversed-start)
               (svg/stitch line-reversed)
               (infinity/path :clockwise
                              [:left :right]
                              [(v/+ row-left
                                    line-reversed-start)
                               (v/+ row-right
                                    line-reversed-start)])
               "z"]
              [top-left row-right]]
        ;; TODO: counterchanged
        ;; field (if (:counterchanged? field)
        ;;         (counterchange/counterchange-field ordinary parent)
        ;;         field)
        #_#_{:keys [cottise-1
                    cottise-2]} (-> ordinary :cottising)]
    [:<>
     [field-shared/make-subfield
      (conj path :field) part
      :all
      environment context]
     (line/render line [line-reversed-data] row-right outline? context)
     #_(when (:enabled? cottise-1)
         (let [cottise-1-data (:cottise-1 cottising)
               fess-base {:type :heraldry.ordinary.type/fess
                          :line (:line cottise-1)
                          :opposite-line (:opposite-line cottise-1)}
               fess-options (ordinary-options/options fess-base)
               {:keys [line
                       opposite-line]} (options/sanitize fess-base fess-options)]
           [fess/render (-> fess-base
                            (merge {:outline? (-> ordinary :outline?)
                                    :field (:field cottise-1)
                                  ;; swap line/opposite-line because the cottise fess is upside down
                                    :line opposite-line
                                    :opposite-line line
                                    :cottising {:cottise-opposite-1 cottise-2}
                                    :geometry {:size (:thickness cottise-1-data)}
                                    :origin {:point :top
                                             :offset-y (-> top
                                                           :y
                                                           (- row)
                                                           (+ line-reversed-min)
                                                           (/ height)
                                                           (* 100)
                                                           (- (:distance cottise-1-data)))
                                             :alignment :left}})) parent environment
            (-> context
                (assoc :override-shared-start-x shared-start-x)
                (assoc :override-real-start real-start)
                (assoc :override-real-end real-end))]))]))
