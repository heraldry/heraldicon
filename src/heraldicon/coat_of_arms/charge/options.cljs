(ns heraldicon.coat-of-arms.charge.options
  (:require
   [heraldicon.coat-of-arms.charge.interface :as charge.interface]
   [heraldicon.coat-of-arms.charge.type.annulet :as annulet]
   [heraldicon.coat-of-arms.charge.type.billet :as billet]
   [heraldicon.coat-of-arms.charge.type.crescent :as crescent]
   [heraldicon.coat-of-arms.charge.type.escutcheon :as escutcheon]
   [heraldicon.coat-of-arms.charge.type.fusil :as fusil]
   [heraldicon.coat-of-arms.charge.type.lozenge :as lozenge]
   [heraldicon.coat-of-arms.charge.type.mascle :as mascle]
   [heraldicon.coat-of-arms.charge.type.roundel :as roundel]
   [heraldicon.coat-of-arms.charge.type.rustre :as rustre]
   [heraldicon.coat-of-arms.charge.type.star :as star]
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]
   [heraldicon.util :as util]))

(def charges
  [roundel/charge-type
   annulet/charge-type
   billet/charge-type
   escutcheon/charge-type
   lozenge/charge-type
   fusil/charge-type
   mascle/charge-type
   rustre/charge-type
   star/charge-type
   crescent/charge-type])

(def choices
  (->> charges
       (map (fn [key]
              [(charge.interface/display-name key) key]))))

(def choice-map
  (util/choices->map choices))

(def type-option
  {:type :choice
   :choices choices
   :ui {:label :string.option/type
        :form-type :charge-type-select}})

;; TODO: part-of-semy? and part-of-charge-group? got lost somewhere along the way,
;; need to be considered again
(defn post-process-options [options context & {:keys [part-of-semy?
                                                      part-of-charge-group?]}]
  (let [ornament? (some #(= % :ornaments) (:path context))
        without-anchor? (or part-of-semy?
                            part-of-charge-group?)]
    (-> options
        (cond->
          without-anchor? (dissoc :anchor)
          ornament? (update-in [:geometry :size] (fn [size]
                                                   (when size
                                                     (assoc size
                                                            :min 5
                                                            :max 400
                                                            :default 100))))))))

(defmethod interface/options-subscriptions :heraldry.component/charge [_context]
  #{[:type]
    [:escutcheon]
    [:orientation :point]
    [:num-points]
    [:variant]
    [:data]
    [:fimbriation :mode]})

(defmethod interface/options :heraldry.component/charge [context]
  (-> context
      (assoc :dispatch-value (charge.interface/effective-type context))
      interface/options
      (assoc :type type-option)
      (assoc :manual-blazon options/manual-blazon)
      (post-process-options context)))

(defn title [context]
  (let [charge-type (interface/get-raw-data (c/++ context :type))
        attitude (or (interface/get-raw-data (c/++ context :attitude))
                     :none)
        facing (or (interface/get-raw-data (c/++ context :facing))
                   :none)
        charge-name (or (choice-map charge-type)
                        (util/translate-cap-first charge-type))]
    (util/combine " " [charge-name
                       (when-not (= attitude :none)
                         (util/translate attitude))
                       (when-not (#{:none :to-dexter} facing)
                         (util/translate facing))])))
