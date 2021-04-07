(ns heraldry.coat-of-arms.default)

(def field
  {:type     :heraldry.field.type/plain
   :tincture :none})

(def ordinary
  {:type  :heraldry.ordinary.type/pale
   :line  {:type :straight}
   :field field
   :hints {:outline? true}})

(def charge
  {:type     :heraldry.charge.type/roundel
   :field    field
   :tincture {:shadow    1
              :highlight 1}
   :hints    {:outline-mode :keep}})

(def coat-of-arms
  {:spec-version 1
   :escutcheon   :heater
   :field        field})

