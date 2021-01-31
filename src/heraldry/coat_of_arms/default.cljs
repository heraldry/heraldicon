(ns heraldry.coat-of-arms.default)

(def content
  {:tincture :none})

(def field
  {:component :field
   :content content})

(def ordinary
  {:component :ordinary
   :type :pale
   :line {:type :straight}
   :field field
   :hints {:outline-mode :keep}})

(def charge
  {:component :charge
   :type :roundel
   :field field
   :hints {:outline-mode :keep}})

(def coat-of-arms
  {:component :coat-of-arms
   :escutcheon :heater
   :field field})
