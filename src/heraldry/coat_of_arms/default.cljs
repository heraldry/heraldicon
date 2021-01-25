(ns heraldry.coat-of-arms.default)

(def content
  {:tincture :none})

(def field
  {:component :field
   :content   content})

(def ordinary
  {:component :ordinary
   :type      :pale
   :line      {:type :straight}
   :field     field
   :hints     {:outline? true}})

(def charge
  {:component :charge
   :type      :roundel
   :variant   :default
   :field     field
   :hints     {:outline? true}})

(def coat-of-arms
  {:component  :coat-of-arms
   :escutcheon :heater
   :field      field})
