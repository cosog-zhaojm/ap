package com.cosog.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
/**
 *  <p>描述：采集组实体类  tbl_acq_group_conf</p>
 *  
 * @author zhao  2018-11-02
 *
 */
@Entity
@Table(name = "tbl_acq_group_conf")
public class AcquisitionGroup implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Integer id;
	private String groupCode;
	private String groupName;
	private Integer acqCycle;
	private Integer saveCycle;
	private String protocol;
	
	private String remark;

	// Constructors

	/** default constructor */
	public AcquisitionGroup() {
	}

	/** full constructor */
	public AcquisitionGroup(String groupCode, String groupName,String remark) {
		this.groupCode = groupCode;
		this.groupName = groupName;
		this.remark=remark;
	}

	@Id
	@GeneratedValue
	@Column(name = "id", unique = true, nullable = false, precision = 10, scale = 0)
	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name = "GROUP_CODE", nullable = false, length = 20)
	public String getGroupCode() {
		return this.groupCode;
	}

	public void setGroupCode(String groupCode) {
		this.groupCode = groupCode;
	}

	@Column(name = "GROUP_NAME", nullable = false, length = 40)
	public String getGroupName() {
		return this.groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	
	@Column(name = "acq_Cycle", precision = 22, scale = 0)
	public Integer getAcqCycle() {
		return acqCycle;
	}

	public void setAcqCycle(Integer acqCycle) {
		this.acqCycle = acqCycle;
	}

	@Column(name = "save_Cycle", precision = 22, scale = 0)
	public Integer getSaveCycle() {
		return saveCycle;
	}

	public void setSaveCycle(Integer saveCycle) {
		this.saveCycle = saveCycle;
	}
	
	@Column(name = "REMARK", nullable = true, length = 10)
	public String getRemark() {
		return this.remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}
	
	@Column(name = "protocol", nullable = true, length = 10)
	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

}