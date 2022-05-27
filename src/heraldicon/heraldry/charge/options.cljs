(ns heraldicon.heraldry.charge.options
  (:require
   [heraldicon.blazonry :as blazonry]
   [heraldicon.context :as c]
   [heraldicon.heraldry.charge.interface :as charge.interface]
   [heraldicon.heraldry.charge.type.annulet :as annulet]
   [heraldicon.heraldry.charge.type.billet :as billet]
   [heraldicon.heraldry.charge.type.crescent :as crescent]
   [heraldicon.heraldry.charge.type.escutcheon :as escutcheon]
   [heraldicon.heraldry.charge.type.fusil :as fusil]
   [heraldicon.heraldry.charge.type.lozenge :as lozenge]
   [heraldicon.heraldry.charge.type.mascle :as mascle]
   [heraldicon.heraldry.charge.type.roundel :as roundel]
   [heraldicon.heraldry.charge.type.rustre :as rustre]
   [heraldicon.heraldry.charge.type.star :as star]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.options :as options]))

(derive :heraldry.charge/type :heraldry/charge)

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

(def ^:private choices
  (map (fn [key]
         [(charge.interface/display-name key) key])
       charges))

(def choice-map
  (options/choices->map choices))

(def ^:private type-option
  {:type :choice
   :choices choices
   :ui {:label :string.option/type
        :form-type :charge-type-select}})

;; TODO: part-of-semy? and part-of-charge-group? got lost somewhere along the way,
;; need to be considered again
(defn- post-process-options [options context & {:keys [part-of-semy?
                                                       part-of-charge-group?]}]
  (let [ornament? (some #(= % :ornaments) (:path context))
        without-anchor? (or part-of-semy?
                            part-of-charge-group?)]
    (cond-> options
      without-anchor? (dissoc :anchor)
      ornament? (update-in [:geometry :size] (fn [size]
                                               (when size
                                                 (assoc size
                                                        :min 5
                                                        :max 400
                                                        :default 100)))))))

(derive :heraldry/charge :heraldry.options/root)

(defmethod interface/options-subscriptions :heraldry/charge [_context]
  #{[:type]
    [:escutcheon]
    [:orientation :point]
    [:num-points]
    [:variant]
    [:data]
    [:fimbriation :mode]})

(defmethod interface/options :heraldry/charge [context]
  (-> context
      charge.interface/options
      (assoc :type type-option
             :manual-blazon options/manual-blazon)
      (post-process-options context)))

(defn title [context]
  (let [charge-type (interface/get-raw-data (c/++ context :type))
        attitude (or (interface/get-raw-data (c/++ context :attitude))
                     :none)
        facing (or (interface/get-raw-data (c/++ context :facing))
                   :none)
        charge-name (or (choice-map charge-type)
                        (blazonry/translate-cap-first charge-type))]
    (string/combine " " [charge-name
                         (when-not (= attitude :none)
                           (blazonry/translate attitude))
                         (when-not (#{:none :to-dexter} facing)
                           (blazonry/translate facing))])))
