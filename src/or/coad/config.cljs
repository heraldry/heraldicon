(ns or.coad.config)

(def placeholder-colours
  {:primary        "#214263"
   :armed          "#848401"
   :langued        "#840101"
   :attired        "#010184"
   :unguled        "#018401"
   :eyes-and-teeth "#848484"})

(def placeholder-colours-set
  (-> placeholder-colours vals set))

(def default-content
  {:tincture :none})

(def default-field
  {:component :field
   :content   default-content})

(def default-ordinary
  {:component :ordinary
   :type      :pale
   :line      {:style :straight}
   :field     default-field})

(def default-charge
  {:component :charge
   :type      :roundel
   :variant   :default
   :field     (-> default-field
                  (assoc :inherit-environment? true))
   :hints     {:outline? true}})

(def default-coat-of-arms
  {:escutcheon :heater
   :field      (-> default-field
                   (assoc-in [:ui :open?] true))})
