(ns heraldry.coat-of-arms.charge.options
  (:require
   [heraldry.coat-of-arms.charge.interface :as charge-interface]
   [heraldry.coat-of-arms.charge.type.annulet :as annulet]
   [heraldry.coat-of-arms.charge.type.billet :as billet]
   [heraldry.coat-of-arms.charge.type.crescent :as crescent]
   [heraldry.coat-of-arms.charge.type.escutcheon :as escutcheon]
   [heraldry.coat-of-arms.charge.type.fusil :as fusil]
   [heraldry.coat-of-arms.charge.type.lozenge :as lozenge]
   [heraldry.coat-of-arms.charge.type.mascle :as mascle]
   [heraldry.coat-of-arms.charge.type.roundel :as roundel]
   [heraldry.coat-of-arms.charge.type.rustre :as rustre]
   [heraldry.context :as c]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [heraldry.util :as util]))

(def charges
  [roundel/charge-type
   annulet/charge-type
   billet/charge-type
   escutcheon/charge-type
   lozenge/charge-type
   fusil/charge-type
   mascle/charge-type
   rustre/charge-type
   crescent/charge-type])

(def choices
  (->> charges
       (map (fn [key]
              [(charge-interface/display-name key) key]))))

(def choice-map
  (util/choices->map choices))

(def type-option
  {:type :choice
   :choices choices
   :ui {:label (string "Type")
        :form-type :charge-type-select}})

;; TODO: part-of-semy? and part-of-charge-group? got lost somewhere along the way,
;; need to be considered again
(defn post-process-options [options context & {:keys [part-of-semy?
                                                      part-of-charge-group?]}]
  (let [ornament? (some #(= % :ornaments) (:path context))
        without-origin? (or part-of-semy?
                            part-of-charge-group?)]
    (-> options
        (cond->
          without-origin? (dissoc :origin)
          ornament? (update-in [:geometry :size] (fn [size]
                                                   (when size
                                                     (assoc size
                                                            :min 5
                                                            :max 400
                                                            :default 100))))))))

(defmethod interface/options-subscriptions :heraldry.component/charge [_context]
  #{[:type]
    [:escutcheon]
    [:anchor :point]
    [:variant]
    [:data]
    [:fimbriation :mode]})

(defmethod interface/options :heraldry.component/charge [context]
  (-> context
      (assoc :dispatch-value (charge-interface/effective-type context))
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
