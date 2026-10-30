package com.example.gestionstationskii.entities;

import java.io.Serializable;

import javax.persistence.*;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level=AccessLevel.PRIVATE)
@Entity
public class Registration implements Serializable {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	Long numRegistration;
	int numWeek;
	@Column(nullable = false)
	@org.hibernate.annotations.ColumnDefault("false")
	boolean cancelled;
	@ManyToOne
	private PlanningSlot planningSlot;

	@JsonIgnore
	@ManyToOne
    Skier skier;
	@JsonIgnore
	@ManyToOne
	Course course;

	@com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
	public Long getSkierId() { return skier == null ? null : skier.getNumSkier(); }
	@com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
	public Long getCourseId() { return course == null ? null : course.getNumCourse(); }
}
